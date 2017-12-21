package s2;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * This is the main class of the library that implements the s2 file format functionality
 * Created by matjaz on 3/25/16.
 */
public class S2 {
    public static final int MAX_DATA_SIZE = 253;

    public enum MESSAGE_TYPE{
        comment (0x0001),
        version (0x0002),
        specialMessage (0x0004),
        metadata (0x0008),
        EOF (0x0010),
        definition (0x0020),
        timestamp(0x0040),
        streamPacket(0x0080),
        unknown (0x0100),
        error(0x0200);

        private final long mask;

        MESSAGE_TYPE(int m){
            this.mask = m;
        }

        public long getMask() {
            return mask;
        }

        public static long maskAll(){
            return Long.MAX_VALUE;
        }
    }

    // unused so far
    enum Dialect {
        dialect_compact,
        dialect_usingNewlines,
        dialect_numDialects,
        dialect_invalid;
    }

    // enums for the definition line
    enum DefinitionType {
        deftype_leaf        ('0',        "leaf"),
        deftype_struct      ('1',        "struct"),
        deftype_timestamp   ('t',        "timestamp"),
        deftype_invalid     ((char)0xFF, "-invalid-");

        private final byte byteId;
        private final String humanReadableName;

        DefinitionType(char val, String humanReadableName) {
            byteId = (byte)val;
            this.humanReadableName = humanReadableName;
        }

        static DefinitionType convert(byte input) {
            switch (input) {
                case '0': return deftype_leaf;
                case '1': return deftype_struct;
                case 't': return deftype_timestamp;
                default:  return deftype_invalid;
            }
        }

        public String toString() {
            return humanReadableName;
        }
    }

    // enums for the absolute identifier (used in more than one line type)
    public enum AbsoluteId {
        abs_absolute('a',       "absolute"),
        abs_relative('r',       "relative"),
        abs_invalid((char)0xff, "-invalid-");

        public final byte byteId;
        public final String humanReadableName;

        AbsoluteId(char val, String humanReadableName) {
            byteId = (byte)val;
            this.humanReadableName = humanReadableName;
        }

        static AbsoluteId convert(byte input) {
            switch (input) {
                case 'a': return abs_absolute;
                case 'r': return abs_relative;
                default:  return abs_invalid;
            }
        }

        public String toString() {
            return humanReadableName;
        }
    }
    
    static boolean specifiesAbsoluteValue(byte id) { return id == AbsoluteId.abs_absolute.byteId; }
    static boolean specifiesRelativeValue(byte id) { return id == AbsoluteId.abs_relative.byteId; }

    // enums for the value type identifier (used in definitions)
    public enum ValueType {
        vt_char     ('c', "char"),
        vt_integer  ('i', "integer"),
        vt_float    ('f', "float"),
        vt_invalid((char)0xff, "-invalid-");

        public final byte byteId;
        public final String humanReadableName;
        ValueType(char val, String humanReadableName) {
            byteId = (byte)val;
            this.humanReadableName = humanReadableName;
        }

        static ValueType convert(byte input) {
            switch (input) {
                case 'c': return vt_char;
                case 'i': return vt_integer;
                case 'f': return vt_float;
                default:  return vt_invalid;
            }
        }

        public String toString() {
            return humanReadableName;
        }
    }

    public static class SensorDefinition {
        String name;
        String unit;
        public byte resolution;         // in bits per sample
        byte scalarBitPadding;   // if resolution is not multiple of 8, then bits might be padded to each sample to get to multiple of 8 bit size - or bits might be padded for other reasons. In any case the number of bits padded (in MSB area) is specified here.
        byte valueType;          // integer, float, ...
        byte absoluteId;         // absolute, relative, ...
        byte vectorSize;         // 1 for scalars, 0 is reserved
        byte vectorBitPadding;   // if resolution is not multiple of 8, then bits might be padded after scalars elements are bitpacked, to get to multiple of 8 vector size - or bits might be padded for other reasons. In any case the number of bits padded (in MSB area of the vector) is specified here.
        float samplingFrequency;    // 0 for sensors that are not sampled regularly, for timestamps, CRCs, and similar non-sensor entities
        public float k, n;

        public SensorDefinition(String name) {
            // by default, make an absolute scalar sensor with 16 bit resolution, no padding and no sampling frequency
            this.name = name;
            unit = "<Default unit>";
            resolution = 16;
            scalarBitPadding = 0;
            valueType = ValueType.vt_integer.byteId;
            absoluteId = AbsoluteId.abs_absolute.byteId;
            vectorSize = 1;
            vectorBitPadding = 0;
            samplingFrequency = 0;
            k = 1;
            n = 0;
        }
        
        public boolean equalValues(Object o)
        {
        	boolean r = false;
        	if(o instanceof SensorDefinition)
        	{
        		SensorDefinition sd = (SensorDefinition) o;
        		r  = this.absoluteId == sd.absoluteId;
        		r &= this.k == sd.k;
        		r &= this.n == sd.n;
        		r &= this.name.equals(sd.name);
        		r &= this.resolution == sd.resolution;
        		r &= this.samplingFrequency == sd.samplingFrequency;
        		r &= this.scalarBitPadding == sd.scalarBitPadding;
        		r &= this.unit.equals(sd.unit);
        		r &= this.valueType == sd.valueType;
        		r &= this.vectorBitPadding == sd.vectorBitPadding;
        		r &= this.vectorSize == sd.vectorSize;
        	}
        	return r;
        }

        public void setUnit(String unit, float k, float n) {
            this.unit = unit;
            this.k = k;
            this.n = n;
        }

        public void setScalar(int resolution, ValueType valueType, AbsoluteId absoluteId, int padding) {
            if (resolution >= 256)
                throw new RuntimeException("Setting sensor definition error: Scalar resolution too large ("+resolution+"), should be lower " +
                        "than 256");
            if (padding >= 256)
                throw new RuntimeException("Setting sensor definition error: Scalar padding too large ("+padding+"), should be lower than 256");
            this.resolution = (byte)resolution;
            this.valueType = valueType.byteId;
            this.absoluteId = absoluteId.byteId;
            this.scalarBitPadding = (byte)padding;
            this.vectorSize = 1;
            this.vectorBitPadding = 0;
        }

        public void setVector(int scalarResolution, ValueType scalarValueType, AbsoluteId absoluteId, int scalarPadding, int vectorSize, int vectorPadding) {
            if (scalarResolution >= 256)
                throw new RuntimeException("Setting sensor definition error: Scalar resolution too large ("+scalarResolution+"), should be lower " +
                        "than 256");
            if (scalarPadding >= 256)
                throw new RuntimeException("Setting sensor definition error: Scalar padding too large ("+scalarPadding+"), should be lower than 256");
            if (vectorSize >= 256)
                throw new RuntimeException("Setting sensor definition error: Vector size too large ("+vectorSize+"), should be lower than 256");
            if (vectorPadding >= 256)
                throw new RuntimeException("Setting sensor definition error: Vector padding too large ("+vectorPadding+"), should be lower than 256");
            this.resolution = (byte)scalarResolution;
            this.valueType = scalarValueType.byteId;
            this.absoluteId = absoluteId.byteId;
            this.scalarBitPadding = (byte)scalarPadding;
            this.vectorSize = (byte)vectorSize;
            this.vectorBitPadding = (byte)vectorPadding;
        }

        public void setSamplingFrequency(float f) {
            samplingFrequency = f;
        }
        public float getSamplingFrequency(){
            return samplingFrequency;
        }

        public float getResolution()  {
            return resolution;
        }

        public String getName(){
            return name;
        }

        public String getUnit() {
            return unit;
        }
    }

    /**
     * Calculate the total bit size of the provided sensor entity (sensor may be scalar/vector, w/o padding, etc)
     * @param sensorDefinition The input sensor
     * @return the number of bits a single sensor entry takes
     */
    static int totalBitSize(SensorDefinition sensorDefinition) {
        return (sensorDefinition.vectorSize * (sensorDefinition.resolution + sensorDefinition.scalarBitPadding)) + sensorDefinition.vectorBitPadding;
    }

    public static class Nanoseconds {
        private long value;

        public Nanoseconds(long val) {
            value = val;
        }

        public Nanoseconds(Nanoseconds copyFrom) {
            value = copyFrom.value;
        }

        public long getValue() {
            return value;
        }

        public static Nanoseconds fromSeconds(double s) {
            return new Nanoseconds((long)(s*1e-9));
        }
    }

    public static class TimestampDefinition {
        byte absoluteId;
        public byte byteSize;
        double multiplier;     // multiply timestamp with this value to get seconds

        public long getNanoMultiplier() { return (long)(multiplier*1e9 + 0.5); }
        /// transform the given timestamp to nanoseconds
        Nanoseconds toNanoSeconds(long stamp) { return new Nanoseconds(getNanoMultiplier() * stamp); }
        public long toImplementationFormat(Nanoseconds nanoStamp) { return nanoStamp.getValue() / getNanoMultiplier(); }
        public TimestampDefinition(AbsoluteId absoluteId, byte byteSize, double multiplier) {
            this.absoluteId = absoluteId.byteId;
            this.byteSize = byteSize;
            this.multiplier = multiplier;
        }
        
        public boolean equalValues(Object o)
        {
        	boolean r = false;
        	if(o instanceof TimestampDefinition)
        	{
        		TimestampDefinition td = (TimestampDefinition) o;
        		r  = this.absoluteId == td.absoluteId;
        		r &= this.byteSize == td.byteSize;
        		r &= this.multiplier == td.multiplier;
        	}
        	return r;
        }
    }

    public static class StructDefinition {
        public String name;
        public String elementsInOrder;

        public StructDefinition(String name, String elementsInOrder) {
            this.name = name;
            this.elementsInOrder = elementsInOrder;
        }
        
        public boolean equalValues(Object o)
        {
        	boolean r = false;
        	if(o instanceof StructDefinition)
        	{
        		StructDefinition sd = (StructDefinition) o;
        		r = this.name.equals(sd.name);
        		r &= this.elementsInOrder.equals(sd.elementsInOrder);
        	}
        	return r;
        }
    }

    public enum MessageType {
        mt_warning      ('w', "warning"),
        mt_error        ('e', "error"),
        mt_exception    ('x', "exception"),
        mt_debug        ('d', "debug"),
        mt_note         ('n', "note"),
        mt_annotation   ('a', "annotation"),

        mt_none         (' ', "-none-"),
        mt_invalid      ((char)0xFF, "-invalid-");

        public final byte byteId;
        public final String humanReadableName;
        MessageType(char val, String humanReadableName) {
            byteId = (byte)val;
            this.humanReadableName = humanReadableName;
        }

        public static MessageType convert(byte input) {
            switch (input) {
                case 'w': return mt_warning;
                case 'e': return mt_error;
                case 'x': return mt_exception;
                case 'd': return mt_debug;
                case 'n': return mt_note;
                case 'a': return mt_annotation;
                case ' ': return mt_none;
                default:  return mt_invalid;
            }
        }

        public String toString() {
            return humanReadableName;
        }
    }

    public enum DeviceType {
        dt_sensorDevice     ((char)0,    "sensing device"),
        dt_recordingDevice  ((char)1,    "recording device"),
        dt_editingDevice    ((char)2,    "editing device"),
        dt_none             (' ',        "-none-"),
        dt_invalid          ((char)0xFF, "-invalid-");

        public final byte byteId;
        public final String humanReadableName;
        DeviceType(char val, String humanReadableName) {
            byteId = (byte)val;
            this.humanReadableName = humanReadableName;
        }

        static DeviceType convert(byte input) {
            switch (input) {
                case 0:   return dt_sensorDevice;
                case 1:   return dt_recordingDevice;
                case 2:   return dt_editingDevice;
                case ' ': return dt_none;
                default:  return dt_invalid;
            }
        }

        public String toString() {
            return humanReadableName;
        }
    }

    // file operation can be either load or store and is set with this variable just after the InternalState initialization;
    // load changes to loadExtra after end-of-file line has been encountered and allows for extra data to be loaded, which is formatted
    // independently of the s2 file format.
    public enum FileOperation {
        op_none,
        op_store,
        op_load,
        op_loadExtra,
        op_invalid;
    }

    private static int lastVersion = 1;
    private static int maxLineBufferLength = 256;

    // notes to the user
    private String notes = "";
    // number of errors encountered while processing s2 requests (should be 0 or the file might not be loaded/stored correctly)
    int numErrors = 0;
    // number of errors encountered while processing s2 requests (should be 0 or the file might not be loaded/stored correctly)
    int numWarnings = 0;
    // internal state that can be returned to store caller
    StoreStatus storeStatus;
    // file used by the storeStatus
    RandomAccessFile file;
    // internal state that can be returned to load caller
    LoadStatus loadStatus;

    public String getFilename() {
        return filename != null ? filename.getName() : "";
    }

    public String getFilePath() {
        return filename != null ? filename.getPath() : "";
    }

    // filename (of load or store functions) is cached here
    File filename;
    // file handle to the open file (either for loading or storing)

    // file version; is 0 until read (load operation) or explicitly set (store operation)
    int fileVersionInteger;
    // file dialect (should apply to each version in a similar manner)
    int fileIntDialect; // unused for now

    @Deprecated
    private long absoluteTimers[] = new long[256]; // array elements are always 0 after the allocation
    // the last read timestamp from timestamps is stored here; variable is initialized to 0 to simplify its usage
    private Nanoseconds lastTimestamp = new Nanoseconds(0);

    // when reading lines, current linenumber should be accessible from here
    long readingLineNum;
    // line number of the last read timestamp (used for circumventing timestamp bug of MobECG < 1.7.8)
    long lastTimestampLineNum = 0;

    // flag (used when reading file) will be set if recording software is detected as MobECG < 1.7.8
    boolean circumventTimestampBugOnRead = false;

    private static final int INVALID_HEADER = 0xFFFF;
    private static final byte NEWLINE = 0x0A; // 0x0A == \n

    S2(File fname) {
        filename = fname;
        fileOperation = FileOperation.op_none;
        fileVersionInteger = 0;
    }

    /**
     * Safely add an error note to the S2 object
     */
    void addErrorNote(String msg) {
        synchronized(this) {
            notes = notes.concat(msg).concat("\n");
            numErrors++;
        }
    }

    /**
     * Safely add a warning note to the S2 object
     */
    void addWarning(String msg) {
        synchronized(this) {
            notes = notes.concat(msg);
            numWarnings++;
        }
    }

    /**
     * Get the text notes (errors, warnings, ...) that were produced since the s2 was started up
     * @return the string containing notes separated by newlines
     */
    public String getNotes() {
        return notes;
    }

    /**
     * Query for the number of encountered warnings; warnings are part of notes and can be obtained via {@link #getNotes()}
     * @return number of warnings in the notes
     */
    public int getNumWarnings() {
        return numWarnings;
    }

    /**
     * Query for the number of encountered errors; errors are part of notes and can be obtained via {@link #getNotes()}
     * @return number of errors in the notes
     */
    public int getNumErrors() {
        return numErrors;
    }

    // a registry of known versions in int
    static Map<Integer, String> versionMap = new HashMap<Integer, String>() {{ put(1, "1.0.0"); }};
    static final int INVALID_VERSION_INT = -1;
    static final String INVALID_VERSION_STRING = "invalid version";

    /**
     * Determine the version string for the supplied int version code
     *
     * param        the integer code to convert
     * return version string this int version code or INVALID_VERSION_INT if the conversion is not successfull (unknown int version code)
     */
    public static String getStringVersion(int version) {
        String s = versionMap.get(version);
        return (s == null ? INVALID_VERSION_STRING : s);
    }

    /**
     * Determine int version code for the supplied version string; performs a reverse search
     *
     * param        the String to convert
     * return integer code for this version string or INVALID_VERSION_STRING if the conversion is not successfull
     */
    public static int getIntVersion(String ver) {
        for (Map.Entry<Integer, String> entry : versionMap.entrySet()) {
            if (ver.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return INVALID_VERSION_INT;
    }

    /**
     * Determine weather the supplied version is valid or not
     * param        the integer version to check
     * return true if the version is known to this library
     */
    boolean isVersionValid(int v) {
        return (versionMap.get(v) != null);
    }

    static final LineType[] lineTypesArray = new LineType[256];
    enum LineType {
        message         ('#',          "message"),
        specialMessage  ('!',          "special message"),
        metadata        ('%',          "metadata"),
        definition      ('d',          "definition"),
        version         ('v',          "version"),
        timestamp       ('t',          "timestamp"),
        endOfFile       ('.',          "end-of-file"),
        invalid         ((char)0x00FF, "-invalid-");

        public final byte byteId;
        public final String humanReadableName;
        LineType(char val, String humanReadableName) {
            byteId = (byte)val;
            lineTypesArray[((int)byteId)&0x00FF] = this;
            this.humanReadableName = humanReadableName;
            //System.out.print((char) byteId +(lineTypesArray[byteId] == null ? " is null" : " is not null") + " in initializer \n");
        }

        public static LineType convert(byte val) {
            return lineTypesArray[val];
        }

        public String toString() {
            return humanReadableName;
        }
    }

    // a registry of known Line types
    static Map<LineType, String> lines;

    FileOperation fileOperation;

    static private int addSizedStringToArray(byte array[], int pos, String string) {
        int newPos = -1;
        if ((pos + string.length() < array.length) && (pos >= 0)) {
            array[pos] = (byte)string.length();
            System.arraycopy(string.getBytes(), 0, array, pos+1, string.length());
            newPos = pos + string.length() + 1;
        }
        return newPos;
    }

    static private int addFloatToArray(byte array[], int pos, float f) {
        return addArrayToArray(array, pos, ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(f).array());
    }

    static private int addIntToArray(byte array[], int pos, long num, int numBytes) {
        return addArrayToArray( array, pos, Arrays.copyOfRange(ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(num).array(), 0,
                numBytes) );
    }

    static private int addDoubleToArray(byte array[], int pos, double f) {
        return addArrayToArray(array, pos, ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putDouble(f).array());
    }

    static private int addArrayToArray(byte array[], int pos, byte array2[]) {
        return addArrayToArray(array, pos, array2, array2.length);
    }

    static private int addArrayToArray(byte array[], int pos, byte array2[], int size) {
        int newPos = -1;
        if ((pos + size <= array.length) && (pos >= 0)) {
            System.arraycopy(array2, 0, array, pos, array2.length);
            newPos = pos + array2.length;
        }
        return newPos;
    }

    public interface ReadLineCallbackInterface {
        boolean onComment(String comment);
        boolean onVersion(int versionInt, String version);
        boolean onSpecialMessage(char who, char what, String message);
        boolean onMetadata(String key,  String value);
        boolean onEndOfFile();
        boolean onUnmarkedEndOfFile();
        boolean onDefinition(byte handle, SensorDefinition definition);
        boolean onDefinition(byte handle, StructDefinition definition);
        boolean onDefinition(byte handle, TimestampDefinition definition);
        boolean onTimestamp(long nanoSecondTimestamp);

        /**
         * Process streaming data packets in this callback
         * @param handle       handle of the data source
         * @param timestamp    the time passed (since measurement start) in nanoseconds
         * @param len          length of the data (number of bytes)
         * @param data         the raw data (this array is not reused and can be directly stored)
         * @return false only if unrecoverable error is encountered in the data
         */
        boolean onStreamPacket(byte handle, long timestamp, int len, byte data[]);
        boolean onUnknownLineType(byte type, int len, byte data[]);
        boolean onError(int lineNum, String error);
    }

    public class StoreStatus {
        // write buffer 'owned' by the writeLine function
        byte writeBuffer[] = new byte[257];
        DeferredWriteBuffer activeDeferredWrite = null;

        /**
         * Default constructor. Does nothing.
         */
        StoreStatus() {
            // initialization has been moved to writeLine, so that the file is not created until first data is to be written
            // initialize(); 
        }
        
        StoreStatus(StoreStatus other) {
        }

        private byte getLineOverhead() {
            return (byte)3;
        }
        
        public DataEntityCache[] getCachedHandles() {
            return cachedHandles;
        }
        
        /**
         * Create a StoreStatus object which will write to previously stored location instead to the file end
         * @param deferredWrite    The DeferredWriteBuffer object which was created previously with @link #deferredWriteLine
         * @return A new StoreStatus object, used only for storing the deferred writes
         * 
         * The deferred write should be finished using the @link #deactivateDeferredWrite function. Adding data to deferred write object should be 
         * wrapped inside try block (if the space for storing runs out, Exception will be thrown)
         * TODO: change from throwing exception to storing an internal error flag and message inside the StoreStatus
         * example of use:
            S2.StoreStatus dfStatus = s2storeStatus.activateDeferredWrite(deferredWriteBuffer);
            try {
                dfStatus.addSpecialTextMessage(S2.dt_recordingDevice, S2.MessageType.mt_annotation, annotation);
            } finally {
                dfStatus.deactivateDeferredWrite();
            }
         */
        public StoreStatus activateDeferredWrite(DeferredWriteBuffer deferredWrite) {
            // create a new Store buffer just for the following write requests
            if ((activeDeferredWrite != null) && (activeDeferredWrite != deferredWrite)) 
                activeDeferredWrite.wrapUp();
            StoreStatus storeDeferred = new StoreStatus(this);
            storeDeferred.activeDeferredWrite = deferredWrite;
            return storeDeferred;
        }

        public StoreStatus deactivateDeferredWrite() {
            if (activeDeferredWrite != null)
                activeDeferredWrite.wrapUp();
            activeDeferredWrite = null;
            return this;
        }
        
        /**
         * Initializes the file and stores file version
         * @return true if everything is ok (even if already initialized), false otherwise (notes contain the error description)
         */
        boolean initialize() {
            // note fileOperation variable is set when running S2.store(...), file is created first calling initialize
            if (file != null)
                return (fileOperation == FileOperation.op_store); // defensive coding, someone called initialize after the file was ended by the user

            try {
                if (fileOperation != FileOperation.op_store) {
                    addErrorNote("Error in StoreStatus.initialize: File "+filename+" has file operation (different than store) applied\n");
                    return false;
                }

                file = new RandomAccessFile(filename, "rw");
                return true;
            } catch (FileNotFoundException e) {
                addErrorNote("Error in StoreStatus.initialize: File "+filename+" was not found / could not be created.\n");
            } catch (NullPointerException e) {
                addErrorNote("Error in StoreStatus.initialize: filename is "+(filename == null ? "null" : filename));
            }
            return false;
        }

        /**
         * function that checks that the stream is valid to write to
         * @return true if the stream is ok
         */
        public boolean isOk() {
            return (fileOperation == FileOperation.op_store);
        }

        /**
         * Execute all pending operations and close the file
         */
        public void done() {
            if ((file != null) && (fileOperation == FileOperation.op_store))
                endFile(true);
        }

        /**
         * Set the file version (must be called prior to all other operations)
         * @param incrementalVersionNumber is the integer number which maps to a valid version string
         * @param extendedVersion a string representing extended file version
         */
        public StoreStatus setVersion(int incrementalVersionNumber, String extendedVersion) {
            if (fileVersionInteger == 0) {
                fileVersionInteger = incrementalVersionNumber;

                if (extendedVersion.isEmpty()) {
                    writeLine((byte)'v', versionMap.get(fileVersionInteger));
                } else {
                    writeLine((byte) 'v', versionMap.get(fileVersionInteger) + " " + extendedVersion);
                }
            } else {
                addWarning("Warning: Set version called after the version was already set (either called twice, or was not called as the " +
                        "very first operation after store).\n");
            }
            return this;
        }

        /**
         * Get the text notes (errors, warnings, ...) that were produced since the s2 was started up
         * @return the string containing notes separated by newlines
         */
        public String getNotes() {
            return notes;
        }

        /**
         * Add a regular text message (a commnet) to the stream
         * @param message the message string (must not exceed 255 bytes)
         * @return self
         */
        public StoreStatus addTextMessage(String message) {
            writeLine((byte)'#', message);
            return this;
        }

        /**
         * Add a special message to the stream
         * @param deviceType the origin device/software/...
         * @param messageType the message type (debug, warning, note, ...)
         * @param message the message string (which must not exceed 252 bytes)
         * @return self
         */
        public StoreStatus addSpecialTextMessage(DeviceType deviceType, MessageType messageType, String message) {
            try {
                byte[] messageBytes = message.getBytes("UTF-8");
                if (messageBytes.length > 255-3) {
                    addWarning("Cannot store special message: \""+message+"\"because it is too long; length = "+messageBytes.length+" B");
                    return this;
                }
    
                byte buf[] = new byte[255];
                buf[0] = deviceType.byteId;
                buf[1] = messageType.byteId;
                int messageLength = messageBytes.length;
                if (getWriteLineLimit() < (messageLength + 3)) {
                    if (getWriteLineLimit() >= 3) {
                        messageLength = getWriteLineLimit() - 3;
                        addErrorNote("Cannot store special message in full, concatenating the message of length "+messageBytes.length+" B to "+
                                messageLength+" B");
                    } else {
                        addErrorNote("Cannot store special message, current write limit is set to "+getWriteLineLimit()+" B, which is insufficient " +
                                "to even store any special message, even an empty one");
                        return this;
                    }
                }
                
                buf[2] = (byte)messageLength;
                System.arraycopy(messageBytes, 0, buf, 3, messageLength);
                writeLine((byte)'!', buf, (byte)(buf[2]+3), -1);
            } catch (UnsupportedEncodingException e) {
                addWarning("Cannot store special message: \""+message+"\"because of UnsupportedEncodingException: "+e.getMessage());
            }
            return this;
        }

        public StoreStatus addSpecialTextMessage(byte deviceType, MessageType messageType, String message, long position) {
            if (message.length() > 255-3) {
                addWarning("Cannot store special message: \""+message+"\"because it is too long; length = "+message.length());
                return this;
            }

            byte buf[] = new byte[255];
            buf[0] = deviceType;
            buf[1] = messageType.byteId;
            buf[2] = (byte)message.length();
            try {
                System.arraycopy(message.getBytes("UTF-8"), 0, buf, 3, message.length());
            } catch (UnsupportedEncodingException e) {
                addWarning("Cannot store special message: \""+message+"\"because of UnsupportedEncodingException: "+e.getMessage());
            }
            writeLine((byte)'!', buf, (byte)(message.length()+3), position);
            return this;
        }

        /**
         * Add metadata to this stream; metadata consists of a key and value string
         * Note that both provided strings together may not exceed 254 characters
         * @param key the metadata key (must not include symbol '=')
         * @param value the metadata value (restricted only by size)
         * @return self
         */
        public StoreStatus addMetadata(String key, String value) {
            writeLine((byte)'%', key+"="+value);
            return this;
        }

        /**
         * Add a sensor definition to stream
         * @param handle the handle to be assigned this definition
         * @param sensorDefinition the definition
         * @return self
         */
        public StoreStatus addDefinition(byte handle, SensorDefinition sensorDefinition) {
            byte buffer[] = new byte[255];
            buffer[0] = handle;
            buffer[1] = DefinitionType.deftype_leaf.byteId;
            int intPos = 2;
            intPos = addSizedStringToArray(buffer, intPos, sensorDefinition.name);
            intPos = addSizedStringToArray(buffer, intPos, sensorDefinition.unit);
            int remainingBytesRequired = 18;
            if ((intPos < 0) && (intPos+remainingBytesRequired < 256)) {
                addErrorNote("S2.StoreStatus.addDefinition failed because of too long input strings when adding sensor definition with name "+
                        sensorDefinition.name+", and unit "+ sensorDefinition.unit+"\n");
            }
            buffer[intPos  ] = sensorDefinition.resolution;
            buffer[intPos+1] = sensorDefinition.scalarBitPadding;
            buffer[intPos+2] = sensorDefinition.valueType;
            buffer[intPos+3] = sensorDefinition.absoluteId;
            buffer[intPos+4] = sensorDefinition.vectorSize;
            buffer[intPos+5] = sensorDefinition.vectorBitPadding;
            intPos += 6;
            intPos = addFloatToArray(buffer, intPos, sensorDefinition.samplingFrequency);
            intPos = addFloatToArray(buffer, intPos, sensorDefinition.k);
            intPos = addFloatToArray(buffer, intPos, sensorDefinition.n);
            if ((intPos < 0) || (intPos > 255)) {
                addErrorNote("S2.StoreStatus.addDefinition failed because of too long input strings when adding sensor definition with name "+
                        sensorDefinition.name+", and unit "+ sensorDefinition.unit+"\n");
            }
            writeLine(LineType.definition.byteId, buffer, (byte)intPos, -1);

            DataEntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new DataEntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copySensorDefinition(sensorDefinition);

            return this;
        }

        /**
         * Add a structure definition to stream
         * @param handle the handle to be assigned this definition
         * @param structDefinition the definition
         * @return self
         */
        public StoreStatus addDefinition(byte handle, StructDefinition structDefinition) {
            byte buffer[] = new byte[255];
            buffer[0] = handle;
            buffer[1] = DefinitionType.deftype_struct.byteId;
            int intPos = 2;
            intPos = addSizedStringToArray(buffer, intPos, structDefinition.name);
            intPos = addSizedStringToArray(buffer, intPos, structDefinition.elementsInOrder);
            if ((intPos < 0) || (intPos > 255)) {
                addErrorNote("S2.StoreStatus.addDefinition failed because of too long input strings when adding struct definition with name "+
                        structDefinition.name+", and elements in order "+ structDefinition.elementsInOrder+"\n");
            }
            writeLine(LineType.definition.byteId, buffer, (byte) intPos, -1);

            DataEntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new DataEntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copyStructDefinition(structDefinition);

            return this;
        }

        /**
         * Add a timestamp definition to stream
         * @param handle the handle to be assigned this definition
         * @param timestampDefinition the definition
         * @return self
         */
        public StoreStatus addDefinition(byte handle, TimestampDefinition timestampDefinition) {
            byte buffer[] = new byte[255];
            buffer[0] = handle;
            buffer[1] = DefinitionType.deftype_timestamp.byteId;
            buffer[2] = timestampDefinition.absoluteId;
            buffer[3] = timestampDefinition.byteSize;
            int size = addDoubleToArray(buffer, 4, timestampDefinition.multiplier);
            writeLine((byte)'d', buffer, (byte)size, -1);

            DataEntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new DataEntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copyTimestampDefinition(timestampDefinition, new Nanoseconds(0));

            return this;
        }

        /**
         * Add a sensor pre-formatted data packet to the stream
         * Throws an exception if something is not correctly set up
         * @param t the "handle" or type of supplied data
         * @param writeReadyTime the timestamp converted into correct units for this handle
         * @param data the data bytes
         * @return self
         */
        public StoreStatus addSensorPacket(byte t, long writeReadyTime, byte[] data) {
            DataEntityCache entity = cachedHandles[t];
            if (entity != null) {
                byte buffer[] = new byte[255];
                if (entity.timestampDefinition != null) {
                    int pos = addIntToArray(buffer, 0, writeReadyTime, entity.timestampDefinition.byteSize);
                    for (int i = 0; i < data.length; ++i) {
                        buffer[pos] = data[i];
                        pos++;
                    }
                    if (pos < 255)
                        writeLine(t, buffer, (byte)pos, -1);
                    else
                        throw new RuntimeException("Error in S2.addSensorPacket: supplied data is too long ("+data.length+") bytes");
                } else {
                    throw new RuntimeException("Error in S2.addSensorPacket: handle " + Integer.toHexString(t) + " does not have a timestamp " +
                            "definition");
                }
            } else {
                throw new RuntimeException("Error in S2.addSensorPacket: handle "+Integer.toHexString(t) + " is not defined yet");
            }

            return this;
        }

        /**
         * Add a sensor pre-formatted data packet to the stream
         * Throws an exception if something is not correctly set up
         * @param t the "handle" or type of supplied data
         * @param writeReadyTime the timestamp converted into correct units for this handle
         * @param data the data bytes
         * @return self
         */
        public StoreStatus addSensorPacket(byte t, long writeReadyTime, List<Byte> data) {
            byte buffer[] = new byte[255];

            DataEntityCache entity = cachedHandles[t];
            if (entity != null) {
                if (entity.timestampDefinition != null) {
                    int pos = addIntToArray(buffer, 0, writeReadyTime, entity.timestampDefinition.byteSize);
                    for (int i = 0; i < data.size(); ++i) {
                        buffer[pos] = data.get(i);
                        pos++;
                    }
                    if (pos < 255)
                        writeLine(t, buffer, (byte)pos, -1);
                    else
                        throw new RuntimeException("Error in S2.addSensorPacket: supplied data is too long ("+data.size()+") bytes");
                } else {
                    throw new RuntimeException("Error in S2.addSensorPacket: handle " + Integer.toHexString(t) + " does not have a timestamp " +
                            "definition");
                }
            } else {
                throw new RuntimeException("Error in S2.addSensorPacket: handle "+Integer.toHexString(t) + " is not defined yet");
            }

            return this;
        }

		public void addStreamPacketRelative(byte handle, long rawTimestamp, byte[] rawData, S2.StoreStatus s) {
            // store data to streaming data file:
			try {
                // store data to streaming data file:
				long previousTimestamp = 0;

                // read previous timestamp value and cache new timestamp value
                DataEntityCache dec = cachedHandles[handle];
                if (dec != null) {
                    previousTimestamp = dec.lastAbsTimestamp.getValue();
                    dec.lastAbsTimestamp = new S2.Nanoseconds(rawTimestamp);
                }

                // TODO: make this work for all timestamp definitions (it should not be hardcoded)
	            // calculate time difference
	            long d = rawTimestamp - previousTimestamp;
	            long dFormatted = (d / 1000L); //(long)(d * multiplier);
	            if (dFormatted < 256L*256L*128L) {
	                // timestamp diff can fit inside a normal message; offset is used here instead of the assignment (lastReceivedTime = receivedTime)
	                // to account for the rounding effects of storing less precise times 
	            	previousTimestamp = (Math.round(dFormatted/1000));
	            } else {
	                // timestamp diff is too large, add a timestamp (accurate one), then a sensor packet with offset 0
	                //NanoTimeDiff offsetFromStart =  new NanoTimeDiff(firstReceivedTime, receivedTime);
	                s.addTimestamp(new S2.Nanoseconds(rawTimestamp));
					dFormatted = 0;
	            }
				
				s.addSensorPacket(handle, dFormatted, rawData);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				//Log.e(LOGTAG, "Error adding samples to debug or stream file", e);
				e.printStackTrace();
			}
        }

        /**
         * Add a full 64bit timestamp to the stream
         * @param nanosFromStart the timestamp in nanoseconds
         * @return self
         */
        public StoreStatus addTimestamp(Nanoseconds nanosFromStart) {
            writeLine((byte) 't', ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(nanosFromStart.getValue()).array());
            return this;
        }

        /**
         * End the file without closing it (this allows further data to be stored in, unformatted)
         * @return self
         */
        public StoreStatus endFile() {
            return endFile(false);
        }

        /**
         * End the file and optionally also close it
         * @param closeFile declares wether the file should also be closed (releasing the resources) or not (allowing additional unformatted writing)
         * @return self
         */
        public StoreStatus endFile(boolean closeFile) {
            byte endLen = 0;
            writeLine((byte)'.', endLen);
            synchronized (file) {
                try {
                    // leave the file open for additional unformatted writing?
                    if (closeFile)
                        file.close();
                    // declare that this file accepts no more s2 formatted data
                    fileOperation = FileOperation.op_none;
                } catch (IOException e) {
                    addErrorNote("Error in ending file (StoreStatus.endFile): " + e.getMessage());
                }
            }
            return this;
        }

        boolean DEBUGWriteLine = false;

        /**
         * Write a line (type @link #t) to file with a single byte @link data as data
         * @param t       type of the line
         * @param data    raw data as a single byte
         */
        void writeLine(byte t, byte data) {
            byte buf[] = new byte[1];
            buf[0] = data;
            if (DEBUGWriteLine) System.out.print("WriteLine(byte t, byte data), byte=" + data + "\n");
            writeLine(t, buf);
        }

        /**
         * Write a line (type @link #t) to file with string @link #data as data
         * @param t       type of the line
         * @param data    raw data encoded as string
         */
        void writeLine(byte t, String data) {
            try {
                if (DEBUGWriteLine) System.out.print("WriteLine(byte t, String data), string="+data+"\n");
                writeLine(t, data.getBytes("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                addErrorNote("Error in writing line (StoreStatus.writeLine); line style = "+(t > 32 ? (char)t : (int)t)+", data = "+data+".\n");
            }
        }

        /**
         * Write a line (type @link #t) to file with byte[] as data (size is determined from @link #data)
         * @param t       type of the line (opcode)
         * @param data    raw data (will be copied verbatim in full length)
         */
        void writeLine(byte t, byte data[]) {
            if (data.length > 255)
                throw new RuntimeException("Error in S2.writeLine: data length = "+data.length+", when max data length is 255");
            writeLine(t, data, (byte)data.length, -1);
        }

        /**
         * Query for the number of bytes that can be written to the stream at its current position.
         *
         * @return the number of bytes that may be written; only deferred write imposes limits on number of bytes
         */
        int getWriteLineLimit() {
            // check if deferred write is active on this StoreStatus
            if (activeDeferredWrite != null) {
                int payloadMaxSize = activeDeferredWrite.getWriteDataLimit()-getLineOverhead();
                return payloadMaxSize;
            }
            return S2.MAX_DATA_SIZE;
        }

        /**
         * Write a line (type @link #t) to file with byte[@link #dataSize] as data
         * This is the only function which truly writes to the file and it is synchronized (no concurrent writes possible)
         * @param t         is the type part of the header to be used;
         * @param data      is the data part of the line
         * @param dataSize  is the size of the data buffer to be written (must be less or equal to 252)
         * @param position  is the write-to position within the file
         */
        void writeLine(byte t, byte data[], byte dataSize, long position) {
            if ((position == -1) && (activeDeferredWrite != null)) {
                // request storage for data size + line overhead
                position = activeDeferredWrite.requestDataWrite(dataSize+getLineOverhead());
                if (position == -1) {
                    // write to the active deferred buffer is not possible, throw an exception
                    deactivateDeferredWrite();
                    throw new RuntimeException("Could not write line, because a deferred write buffer is active with insufficient space to " +
                            "accommodate the write request, dataSize="+dataSize+", position="+position);
                }
            }
            // check that the file is open for writing or open it right away
            if ((file != null) || initialize()) {
                // check that file version is set or set it here
                if (fileVersionInteger == 0)
                    setVersion(lastVersion, "");

                synchronized (file) {
                    try {
                        if (DEBUGWriteLine) System.out.print("WriteLine(byte t, byte data[]), data.length=" + dataSize + "+2+1\n");
                        if (dataSize+2-1 > 255)
                            throw new IOException("\"Error in writing to file (StoreStatus.writeLine); buffer is too large ("+dataSize+")");

                        // output header
                        writeBuffer[0] = t;
                        writeBuffer[1] = dataSize;
                        int dataSizeInt = dataSize < 0 ? dataSize+256 : dataSize;
                        // output the data
                        System.arraycopy(data, 0, writeBuffer, 2, dataSizeInt);
                        // output the newline
                        writeBuffer[2+dataSizeInt] = NEWLINE;
                        // consume
                        if (position == -1) {
                            // write to current position in file (current position is always at the end-of-file)
                            file.write(writeBuffer, 0, dataSizeInt + 2 + 1);
                        } else {
                            // this will overwrite an arbitrary position within the file and (re)set the write position to end-of-file
                            long lastPos = file.getFilePointer();
                            if (lastPos < position)
                                throw new IOException("\"Error in writing to file (StoreStatus.writeLine) to position "+position+", which is larger"+
                                        " than the current position ("+lastPos+")");
                            file.seek(position);
                            file.write(writeBuffer, 0, dataSizeInt+2+1);
                            file.seek(Math.max(lastPos, file.getFilePointer()));
                        }

                        /*
                        // output the buffer to file
                        file.write(data, 0, ((int)(dataSize) & 0xFF));
                        // add the newline
                        file.write(NEWLINE); // note: nevermind the signature of the write functions, it writes a byte, not an int
                        */
                    } catch (IOException e) {
                        addErrorNote("Error in writing to file (StoreStatus.writeLine); line style = " + (t > 32 ? (char) t : (int) t) + ", length = " +
                                dataSize + ".\n");
                    }
                }
            }
        }

        /**
         * Start the procedure for a deferred write. This will write a dummy line to file and return an object pointing to that line for overwriting.
         * @param dataSize    the size of data to be allocated (the user will have to match this size when writing the line later)
         * @return            the buffer which can be later used for writing
         */
        public DeferredWriteBuffer deferredWriteLine(int dataSize) {
            DeferredWriteBuffer temp = null;
            if (dataSize > S2.MAX_DATA_SIZE) {
                addErrorNote("Error in StoredStatus.deferredWriteLine: dataSize ("+dataSize+") is too large, max = "+S2.MAX_DATA_SIZE);
                return null;
            }
            if (dataSize < getLineOverhead()) {
                addErrorNote("Error in StoredStatus.deferredWriteLine: dataSize ("+dataSize+") is too small, min = "+getLineOverhead());
                return null;
            }
            try {
                byte byteDataSize = (byte)dataSize;
                synchronized (file) {
                        // synchronized this block to keep the newly created deferred write buffer in same location as the newly written comment line
                        // since a bug was found, which manifested itself as if a data line was written just after the new DeferredWriteBuffer line and
                        // before the writeLine('#') line
                        // TODO: dummy line should be written by the DeferredWriteBuffer itself, and not done here!
                    temp = new DeferredWriteBuffer(this, file.getFilePointer(), byteDataSize);
                    // write a dummy line
                    dataSize -= getLineOverhead();
                    if (dataSize == 0) {
                        writeLine((byte) '#', "");
                    } else {
                        byte dummyText[] = new byte[dataSize]; // filled with zeros, which is perfect
                        writeLine((byte) '#', dummyText);
                    }
                }
            } catch (IOException e) {
                addErrorNote("Error in StoredStatus.deferredWriteLine: "+e.getMessage());
            }
            return temp;
        }
    }

    public static class DeferredWriteBuffer {
        WeakReference<StoreStatus> parent;
        byte sizeLimit;
        long writePosition;

        DeferredWriteBuffer(StoreStatus parent, long position, byte dataSize) {
            this.parent = new WeakReference<StoreStatus>(parent);
            this.writePosition = position;
            this.sizeLimit = dataSize;
        }

        /**
         * Query for the maximum number of bytes that can be written 
         * @return  the number of bytes one can write with the call to S2.StoreStatus.writeLine(...)
         */
        synchronized public int getWriteDataLimit() {
            return sizeLimit;
        }
        
        /**
         * Query the buffer if data of supplied #dataSize length may be written to it
         * @param dataSize    the desired length of data (in bytes) to be written
         * @return true if the data may be written, false if data is tool large to fit into the buffer
         */
        synchronized public boolean canWriteData(int dataSize) {
            // dataSize must be smaller then the limit and must not equal limit-1, because no valid line can then be used to fill in the remaining 
            // space
            return (dataSize <= sizeLimit) && (dataSize != (sizeLimit-1));
        }

        /**
         * Request a position for writing the requested amount of data (that is including the line structure overhead).
         * This function will return the buffer position (if the buffer is large enough) and will advance the buffer position by the requested size.
         * Therefore for subsequent writes, one only has to call this function several times.
         * 
         * @param dataSize    The number of bytes to advance the position by.
         * @return the position within the file to write at or -1 if write is not possible
         */
        synchronized public long requestDataWrite(int dataSize) {
            long returnValue = -1;
            if ((dataSize <= sizeLimit-3) || (dataSize == sizeLimit)) {
                returnValue = writePosition;
                sizeLimit -= dataSize;
                writePosition += dataSize;
            } 
            return returnValue;
        }
        
        @Deprecated
        synchronized public long getPosition() {
            return writePosition;
        }

        /**
         * Raw write line to the location in the file pointed by the deferred write buffer
         * @param t           line type  
         * @param data        raw line data
         * @param dataSize    size of #data
         * 
         * No known uses of this function, one should approach writing to deferred write buffer by providing it to the StoreStatus and then
         * writing there in the regular manner
         */
        synchronized void writeLine(byte t, byte data[], byte dataSize) {
            StoreStatus storeStatus = parent.get();
            if (null != storeStatus) {
                storeStatus.writeLine(t, data, dataSize, writePosition);
            }
        }

        /**
         * Wrap up the deferred write request (either after a write has been made or not); This function will make sure the reserved space in the 
         * file is properly formatted so that the file is conforming to the format and in a readable state
         */
        synchronized public boolean wrapUp() {
            // add an empty comment at the end of the buffer, taking up the rest of the buffer space
            final byte emptyData[] = new byte[253];
            StoreStatus sts = parent.get();
            if (sizeLimit >= sts.getLineOverhead())
                writeLine((byte) '#', emptyData, (byte)(sizeLimit-sts.getLineOverhead())); 
            return (sizeLimit >= sts.getLineOverhead()) || (sizeLimit == 0);
            // at the end of the function, the state of deferred buffer is left unchanged, therefore allowing additional writes, but these 
            // additional writes must be then again completed by a call to wrapUp
        }
    }

    /**
     * A helper class for parsing the raw byte data.
     * TODO: make all process function use it internally and as a means of exposed input arguments
     */
    protected static class BufferParseState {
        final int bufferLength;
        final byte[] buffer;
        int bufferIndex;
        String error = null;

        /**
         * Basic constructor that sets all the variables and sets the buffer index to 0
         * @param buffer    raw byte buffer (will not be copied but will also not modify the original)
         * @param len       length of the buffer
         */
        public BufferParseState(final byte[] buffer, int len) {
            this.buffer = buffer;
            this.bufferIndex = 0;
            this.bufferLength = len;
        }

        /**
         * Basic constructor that sets all the variables
         * @param buffer    raw byte buffer (will not be copied but will also not modify the original)
         * @param len       length of the buffer
         * @param index     starting index of the buffer
         */
        public BufferParseState(final byte[] buffer, int len, int index) {
            this.buffer = buffer;
            this.bufferIndex = index;
            this.bufferLength = len;
        }

        /**
         * Query whether an error is flagged; if it is no functions that report error will work.
         * @return true if error flag is on
         */
        public boolean errorFlagged() {
            return error != null;
        }

        /**
         * Parse a non-empty string from buffer. String length must be the first element, followed by the string.
         * Note that if error is set, then parsing will automatically fail.
         *
         * @return The parsed string; will be null on error. Also {@link #error} string will be filled with the description
         */
        public String parseString() {
            if (error != null)
                return null;

            StringBuilder str = new StringBuilder();
            // first sanity check (one could try parsing multiple strings without checking for error...)
            if (bufferIndex < bufferLength) {
                int strLen = buffer[bufferIndex];
                bufferIndex++;
                int maxIndex = strLen + bufferIndex;
                if (maxIndex <= bufferLength) {
                    for (; bufferIndex < maxIndex; bufferIndex++) {
                        str.append((char) buffer[bufferIndex]);
                    }
                } else {
                    error = ("Error in parsing a string: length of the string is specified as "+strLen+
                            ", but the buffer only contains another "+(bufferLength-bufferIndex)+" bytes (total of"+
                            bufferLength+").");
                    return null;
                }
            } else {
                error = ("Error in parsing a string: index inside the buffer is "+bufferIndex+
                        ", while buffer length is "+bufferLength+".");
                return null;
            }
            return str.toString();
        }

        /**
         * Get the raw byte from the raw buffer; errors are not flagged.
         * @return a byte that is on the indexed position of the buffer; 0 on error
         */
        public byte getByte() {
            return (bufferIndex < bufferLength) ? buffer[bufferIndex++] : 0;
        }

        /**
         * Get the raw byte from the raw buffer and encode it as char; errors are not flagged.
         * @return a char (8-bit) that is on the indexed position of the buffer; 0 on error
         */
        public char get8bitChar() {
            return (bufferIndex < bufferLength) ? (char)buffer[bufferIndex++] : '\0'; // TODO: could use a non 8bit char as error indication
        }

        /**
         * Get a binary encoded float from the raw buffer; errors are not flagged.
         * @return a float value that is on the indexed position of the buffer; NAN on error
         */
        public float getFloat() {
            if (bufferIndex <= (bufferLength-4)) {
                float f = ByteBuffer.wrap(buffer, bufferIndex, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                bufferIndex += 4;
                return f;
            } else
                return Float.NaN;
        }

        /**
         * Get a binary encoded double from the raw buffer; errors are not flagged.
         * @return a double value that is on the indexed position of the buffer; NAN on error
         */
        public double getDouble() {
            if (bufferIndex <= (bufferLength-8)) {
                double d = ByteBuffer.wrap(buffer, bufferIndex, 8).order(ByteOrder.LITTLE_ENDIAN).getDouble();
                bufferIndex += 8;
                return d;
            } else
                return Double.NaN;
        }

        /**
         * Clear the error flag and error message; processing will be enabled again.
         */
        public void clearError() {
            error = null;
        }
    }

    public class LoadStatus {
        BufferedInputStream file;
        private long messageMask;

        public void setMessageMask(long messageMask) {
            this.messageMask = messageMask;
        }

        // callbacks for processing lines when reading from s2 file
        private ArrayList<ReadLineCallbackInterface> callbacks = new ArrayList<ReadLineCallbackInterface>();

        LoadStatus() {
            messageMask = Long.MAX_VALUE;

            try {
                //file = new FileInputStream(filename);
                file = new BufferedInputStream(new FileInputStream(filename));
            } catch (FileNotFoundException e) {
                addErrorNote("Error in LoadStatus ctor: File "+filename+" was not found.\n");
                file = null;
            }
        }
        
        public ArrayList<ReadLineCallbackInterface> getCallbacksArray() {
       	    return callbacks;
        }

        /**
         * Check that the stream is valid to read from.
         * @return true if the stream is ok
         */
        public boolean isOk() {
            return (file != null) && (fileOperation == FileOperation.op_load);
        }

        /**
         * @brief Read a single line from file and process it
         * @return true while there are more lines to read, false on marked and unmarked end of file
         */
        boolean processLine(Line line) {
            if (line != null) {
                // is this either a known line type or a registered sensor line?
                if (isValidLine(line.op)) {
                    // type of the line can be one of the predefined line types
                    LineType op = LineType.convert(line.op);
                    if (op != null) switch (op) {
                        case message:
                            if ((messageMask & MESSAGE_TYPE.comment.mask) > 0)
                                return processComment(line.len, line.data);
                            break;
                        case specialMessage:
                            if ((messageMask & MESSAGE_TYPE.specialMessage.mask) > 0)
                                return processSpecialMessage(line.len, line.data);
                            break;
                        case metadata:
                            if ((messageMask & MESSAGE_TYPE.metadata.mask) > 0)
                                return processMetadata(line.len, line.data);
                            break;
                        case definition:
                            if ((messageMask & MESSAGE_TYPE.definition.mask) > 0)
                                return processDefinition(line.len, line.data);
                            break;
                        case version:
                            if ((messageMask & MESSAGE_TYPE.version.mask) > 0)
                                return processVersion(line.len, line.data);
                            break;
                        case timestamp:
                            if ((messageMask & MESSAGE_TYPE.timestamp.mask) > 0)
                                return processTimestamp(line.len, line.data);
                            break;
                        case endOfFile:
                            // discard the return of this function (what should be done about it anyways?)
                            processEndOfFile(line.len, line.data);
                            return false;
                        case invalid:
                            break;
                    } else {
                        if (isSensorHandle(line.op)) {
                            return processDataLine(line.op, line.len, line.data);
                        } else {
                            if ((messageMask & MESSAGE_TYPE.unknown.mask) > 0)
                                return processUnknownLine(line.op, line.len, line.data);
                        }
                    }
                } else {
                    // unknown line type
                    boolean ret = processUnknownLine(line.op, line.len, line.data);
                    if (!ret) {
                        addWarning("Warning, one or more of the 'onUnknownLine' callbacks returned false.\n");
                    }
                    return false;
                }
                return true;
            } else {
                // line is null when end of file is reached while trying to read a new line, this is a so called unmarked end of file
                boolean ret = processUnmarkedEndOfFile();
                if (!ret) {
                    addWarning("Warning, one or more of the 'onUnmarkedEndOfFile' callbacks returned false.\n");
                }
                return false;
            }
        }

        /**
         * Read lines from the file and process them; number of lines and amount of processing is predefined.
         * To define the the line types to be processed, use {@link #setMessageMask(long)}.
         * To define the callback (will be called after processing each line), use {@link #addReadLineCallback(ReadLineCallbackInterface)}
         * return   false if an unrecoverable error occurs (file is not open, etc), true if everything is no exceptions occur
         *          (even if some lines are not recognised)
         */
        public boolean readAndProcessFile() {
            // first, determine if the file was loaded successfully and terminate gracefully if not
            if (file == null) {
                addErrorNote("readAndProcessFile failed, file was not loaded successfully");
                return false;
            }
            boolean endOfFile = false;
            while(!endOfFile) {
          	    Line line = readLine();
                if (line == null)
                    addWarning("Warning, a null line was read (probably because this file is missing the end-of-file line)\n");
                endOfFile = !processLine(line);
            }
            return true;
        }

        /**
         * Use callbacks to parse information from lines that are properly formatted, their types known, and successfully read from the input.
         * Removing a callback is not supported (yet) as there is no known use case for it. There is an option of clearing all callbacks though
         * by calling {@link #clearReadLineCallbacks()}.
         *
         * @param callback the callback for processing one line at a time
         */
        public void addReadLineCallback(ReadLineCallbackInterface callback) {
            callbacks.add(callback);
        }

        /**
         * Get the entity with the supplied handle.
         * @param handle a byte that defines the entity (its handle)
         * @return the entity
         */
        DataEntityCache getDataEntity(byte handle) {
            return cachedHandles[handle];
        }

        /**
         * Remove all read line callbacks
         */
        public void clearReadLineCallbacks() {
            callbacks.clear();
        }

        /**
         * Convert the supplied relative timestamp (usually called dt) to an absolute timestamp and advance the absolute time.
         * Warning: must not call this function with out-of-order timestamps!
         *
         * @param dt The time difference from the previous registered timestamp (either relative or absolute)
         * @return  The 'absolute time', which is just time relative to the measurement start.
         *          All relative timestamps should be converted in the order that they appear in the file.
         */
        @Deprecated
        long relativeToAbsoluteTime(byte handle, long dt) {
            if (handle < absoluteTimers.length) {
                absoluteTimers[handle] += dt;
                return absoluteTimers[handle];
            } else
                return 0;
        }

        /**
         * Single line of the S2 file
         */
        class Line {
            byte op;
            int len;
            byte[] data;
            boolean success;

            /**
             * Define the line through raw data.
             * @param op    operation code of the line; defines the line type
             * @param len   length of the line
             * @param data  the data payload of the line
             */
            Line(byte op, int len, byte[] data) {
                this.op = op;
                this.len = (data == null ? 0 : (byte)data.length);
                success = (this.len == len);
                this.len = len;
                this.data = data;
            }

            /**
             * Instantiate an uninitialized line
             */
            Line() {
                success = false;
                data = null;
            }
        }

        /**
         * Read a single line from file; the number of bytes read is not known in advance
         * @return the Line structure with the raw data
         */
        Line readLine() {
            assert (fileOperation == FileOperation.op_load);
            assert (loadStatus != null);
            assert (maxLineBufferLength > 255);

            ++readingLineNum;
            // read the 2-char header, which is present for all line types
            try {
                byte header[] = new byte[2];
                int numAvail = file.available();
                // make sure that the 2 header bytes are available
                if (numAvail >= 2) {
                    // 2-byte header part of the line can be read
                    int numRead = file.read(header);
                    if (numRead != 2) {
                        // defensive coding - not sure if this situation can arise at all (after all, numAvail is >=2)
                        addErrorNote("Error while reading line "+readingLineNum+": could not read header of line; only " + numRead +
                                " bytes were read, although "+numAvail+" should be available.\n");
                        return new Line(LineType.invalid.byteId, numRead, (numRead > 0 ? Arrays.copyOf(header, numRead) : null));
                    }

                    // read data part of the line
                    int bufLength = ((int)header[1]) & 0xff;	// convert unsigned byte (length cannot be negative) to integer
                    byte buf[] = bufLength == 0 ? null : new byte[bufLength];
                    boolean readOk = (bufLength == 0) || (bufLength == file.read(buf));
                    if (readOk) {
                        // ignore the newline that follows the line
                        // HACK: check if newline is read and if not, read until it is; TODO: fix this in a better way
                        for (int iii = 0; iii < 512; ++iii) {
                            if (file.available() == 0) {
                                addWarning("Warning in line "+readingLineNum+"; missing newline.\n");
                                break;
                            }
                            byte eol = (byte)file.read();
                            if (eol == (byte)'\n') {
                                if (iii > 0) {
                                    addWarning("Warning in line "+readingLineNum+"; type="+
                                            (header[0]>=32 ? ""+(char)header[0] : "#"+(int)header[0])+", len\n");
                                    addErrorNote("Error in line "+readingLineNum+"; LoadStatus.readLine: declared " +
                                            "line is "+iii+" bytes longer than declared ("+bufLength+")\n");
                                }
                                break;
                            }
                        }
                        /*
                        // old implementation - works ok, but cannot deal gracefully with a common error found in
                        // MobECG generated s2 files
                        readOk = (1 == file.skip(1));
                        if (!readOk)
                            addErrorNote("Warning in LoadStatus.readLine: missing newline.\n");
                         */
                    } else {
                        addErrorNote("Error while reading line "+readingLineNum+": could not read full line (length="+(int)header[1]+" bytes.\n");
                    }

                    //that's it, return the line
                    return new Line(header[0], bufLength, buf);
                } else {
                    // 2 bytes are not available; 2 different scenarios are possible - file was terminated incorrectly when writing or was damaged later on
                    if (numAvail == 0) {
                        // very likely the file was written correctly up to this point but the program that was writing it
                        // terminated unexpectedly and could not add end-of-file line
                        addWarning("Warning while reading line "+readingLineNum+": end of file was reached but the end was not marked; this indicates that the file is incomplete - write procedure was interrupted before it finished.\n");
                        return null;
                    } else {
                        // numAvail can only be 1 in this case, indicating a damaged file, since line header cannot be read but end-of-file line was not given yet
                        byte buf[] = new byte[numAvail];
                        int numRead = file.read(buf);

                        addErrorNote("Error while reading line " + readingLineNum + ": could not read header, only "+numAvail+" bytes were available.\n");
                        return new Line(LineType.invalid.byteId, numRead, buf);
                    }
                }
            } catch (IOException e) {
                // IO errors, i.e. errors in processing java streams
                addErrorNote("Error in LoadStatus.readline: IOException caught. readingLineNum="+readingLineNum+"\n");
                return null;
            }
        }

        //region Functions for processing single line type

        /**
         * Process an unknown type of line - raw data is passed to the function which then forwards it unmodified to the callback.
         * @param op        the opcode of line; line type
         * @param len       length of the line data part
         * @param buffer    raw byte buffer of the data part
         * @return union (and operator) of returns from all callbacks
         */
        boolean processUnknownLine(byte op, int len, byte buffer[]) {
        	boolean ret = true;
        	try {
    			for (ReadLineCallbackInterface callback : callbacks)
            		ret &= callback.onUnknownLineType(op, len, buffer);
    		} catch (Exception e) {
    			addWarning("Error while processing unrecognised line");
            	ret = false;
    		}

            return ret;
        }

        /**
         * Process a line that contains a comment.
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processComment(int len, byte buffer []) {
            boolean ret = true;        // comments are always ok
            String comment;
            try {
                comment = len == 0 ? "" : new String(buffer, "UTF-8");
                for (ReadLineCallbackInterface callback : callbacks)
                	ret &= callback.onComment(comment);
            } catch (Exception e) {
                addWarning("Error while processing message/comment");
                ret = false;
            }

            return ret;
        }

        /**
         *
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processSpecialMessage(int len, byte buffer []) {
            boolean ret = true;        // ok by default
            try {
            	char source = (char)buffer[0];	// Device is specified in buffer at position 0
            	char messageTypeChar = (char)buffer[1];	// Message type is specified in buffer at position 1
            	int messageLength = ((int)buffer[2]) & 0xff; // Message length is given in an unsigned byte
                String specialMessage = new String(Arrays.copyOfRange(buffer, 3, messageLength+3), "UTF-8");
            	for(ReadLineCallbackInterface callback : callbacks)
            		ret &= callback.onSpecialMessage(source, messageTypeChar, specialMessage);
            } catch (Exception e) {
            	addWarning("Error while processing special message");
            	ret = false;
            }
            return ret;
        }

        /**
         *
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processMetadata(int len, byte buffer []) {
            boolean ret = true;        // ok by default
            try {
        		String metadata = len == 0 ? "" : new String(buffer, "UTF-8");
        		String[] keyValue = metadata.split("=");
            	for (ReadLineCallbackInterface callback : callbacks)
            		ret &= callback.onMetadata(keyValue[0], keyValue[1]);

                // catch buggy writing software here..
                if (keyValue[0].equals("recording software")) {
                    RecordingSoftware rs = new RecordingSoftware();
                    rs.parse(keyValue[1]);
                    // System.out.printf("sw=%s ver=%d.%d.%d %s\n", rs.software, rs.major, rs.minor, rs.revision, rs.other); // test RecordingSoftware parsing
                    if (rs.software.equals("MobECG") && rs.versionBelow(1, 7, 8)) {
                        circumventTimestampBugOnRead = true;
                    }
                }
            } catch (Exception e) {
                addErrorNote("Error while processing metadata");
                ret = false;
            }

            return ret;
        }

        /**
         *
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processVersion(int len, byte buffer []) {
        	boolean ret = true;        // ok by default

        	try {
    			String intVersion = "";
    			String extendedVersion = "";
    			boolean strVersion = false;
        		for(int i = 0; i < len; i++) {
        			if (buffer[i] == ' ' || (char)buffer[i] == ' ')	// split data to numeric and string
        			{
        				strVersion = true;
        				continue;									// skips ' ' character
        			}
        			if (!strVersion)
        				intVersion += (char)buffer[i];				// Writing numeric data
        			else
        				extendedVersion += (char)buffer[i];			// Writing string data
        		}
        		int toIntVersion = getIntVersion(intVersion);
                //boolean isValid = isVersionValid(toIntVersion);
        		for(ReadLineCallbackInterface callback : callbacks)
        			ret &= callback.onVersion(toIntVersion, extendedVersion);

    		} catch (Exception e) {
    			addWarning("Error while processing version");
            	ret = false;
    		}

            return ret;
        }

        /**
         * Process a sensor definition line (called only from {@link #processDefinition(int, byte[])}).
         *
         * @param handle    the data entity handle
         * @param ps        Parser state that contains buffer, its length and all other required info
         * @return union (and operator) of returns from all callbacks
         */
        boolean processDefinitionLeaf(byte handle, BufferParseState ps) {
            boolean ret = true;

            // parse the name
            String name = ps.parseString();
            String unit = ps.parseString();

            // parse the definitions
            byte resolutionByte = ps.getByte();
            byte bitPaddingByte = ps.getByte();
            byte valueTypeByte = ps.getByte();
            byte absoluteIdByte = ps.getByte();
            byte vectorSizeByte = ps.getByte();
            byte vectorBitPaddingByte = ps.getByte();

            // parse frequency
            float frequencyValue = ps.getFloat();
            float kValue = ps.getFloat();
            float nValue = ps.getFloat();

            if (ps.error != null) {
                processError("Error while parsing sensor definition for data entity #"+handle+": " + ps.error);
                ret = false;
            } else {

                // copy the values found in the definition verbatim; do not check for correctness
                SensorDefinition sd = new SensorDefinition(name);
                sd.setUnit(unit, kValue, nValue);
                sd.resolution = resolutionByte;
                sd.scalarBitPadding = bitPaddingByte;
                sd.valueType = valueTypeByte;
                sd.absoluteId = absoluteIdByte;
                sd.vectorSize = vectorSizeByte;
                sd.vectorBitPadding = vectorBitPaddingByte;
                sd.samplingFrequency = frequencyValue;

                // add the definition to cache of data entries
                DataEntityCache entity = cachedHandles[handle];
                if (entity == null) {
                    // add a new handle
                    entity = new DataEntityCache();
                    cachedHandles[handle] = entity;
                }
                entity.copySensorDefinition(sd);

                // process callbacks
                for (ReadLineCallbackInterface callback : callbacks)
                    ret &= callback.onDefinition(handle, sd);
            }
            return ret;
        }

        /**
         * Process a structure definition line (called only from {@link #processDefinition(int, byte[])}).
         *
         * @param handle    the data entity handle
         * @param ps        Parser state that contains buffer, its length and all other required info
         * @return union (and operator) of returns from all callbacks
         */
        boolean processDefinitionStruct(byte handle, BufferParseState ps) {
            boolean ret = true;

            // parse structure name and its elements
            String name = ps.parseString();
            String elements = ps.parseString();
            if (ps.errorFlagged()) {
                processError("Error while parsing sensor structure for data entity #"+handle+": " + ps.error);
                ret = false;
            }

            // add the structure definition to the cache of data entities; no checking is performed for correctness of elements
            StructDefinition sdef = new StructDefinition(name, elements);
            DataEntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new DataEntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copyStructDefinition(sdef);

            // process callbacks
            for (ReadLineCallbackInterface callback : callbacks)
                ret &= callback.onDefinition(handle, sdef);
            return ret;
        }

        /**
         * Process a timestamp definition line (called only from {@link #processDefinition(int, byte[])}).
         *
         * @param handle    the data entity handle
         * @param ps        Parser state that contains buffer, its length and all other required info
         * @return union (and operator) of returns from all callbacks
         */
        boolean processDefinitionTimestamp(byte handle, BufferParseState ps) {
            boolean ret = true;
            byte absoluteIdChar = ps.getByte();
            byte byteSizeValue = ps.getByte();
            double resolutionTs = ps.getDouble();

            if (ps.error != null) {
                processError("Error while parsing timestamp definition for data entity #"+handle+": "+ps.error);
                ret = false;
            } else {
                AbsoluteId ai = AbsoluteId.convert(absoluteIdChar);
                TimestampDefinition td = new TimestampDefinition(ai, byteSizeValue, resolutionTs);

                DataEntityCache entity = cachedHandles[handle];
                if (entity == null) {
                    // add a new handle
                    entity = new DataEntityCache();
                    cachedHandles[handle] = entity;
                }
                // set timestamp within the definition to last explicit timestamp (0 at the beginning)
                // if circumventing the timestamp bug, then the definitions always start at time 0
                entity.copyTimestampDefinition(td, circumventTimestampBugOnRead ? new Nanoseconds(0) : lastTimestamp);

                for (ReadLineCallbackInterface callback : callbacks)
                    ret &= callback.onDefinition(handle, td);
            }
            return ret;
        }

        /**
         * Process a definition line type (subtype can be timestamp, struct or sensor).
         * The function will call an appropriate processDefinitionX function
         *
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processDefinition(int len, byte buffer []) {
        	boolean ret = true;           // ok by default

            BufferParseState ps = new BufferParseState(buffer, len);
            if (len < 2) {
                ret = false;
                processError("Error in definition line, data part of message is too short ("+len+" bytes long).");
            } else {
                byte handle = ps.getByte();      // At position 0 is specified the target handle
                DefinitionType dt = DefinitionType.convert(ps.getByte()); // Type definition is specified in buffer at position 1
                try {
                    switch (dt) {
                        case deftype_leaf:
                            ret = processDefinitionLeaf(handle, ps);
                            break;
                        case deftype_struct:
                            ret = processDefinitionStruct(handle, ps);
                            break;
                        case deftype_timestamp:
                            ret = processDefinitionTimestamp(handle, ps);
                            break;
                    }
                } catch (Exception e) {
                    addErrorNote("Error while processing definition");
                    ret = false;
                }
            }

            return ret;
        }

        /**
         * Process a timestamp line type.
         *
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processTimestamp(int len, byte buffer []) {
            boolean ret = true;        // ok by default
            //TODO this timestamp does not have handle but correct all timestamps for each handle in this value
            try {
                lastTimestamp = new Nanoseconds(ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong());
                lastTimestampLineNum = readingLineNum;

                // copy the timestamp to current timestamp values of all known sensors
                // Note: sensor definition must be complete before the timestamp is recorded
                if (!circumventTimestampBugOnRead)
                    copyTimestampToDefinitions();

    			for (ReadLineCallbackInterface callback : callbacks)
        			ret &= callback.onTimestamp(lastTimestamp.getValue());
            } catch (Exception e) {
                addErrorNote("Error while processing timestamp");
                ret = false;
            }

            return ret;
        }

        /**
         * Process an end-of-file line. Processing will stop after this line, no more s2 lines can follow.
         *
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processEndOfFile(int len, byte buffer []) {
            boolean ret = true;

            try {
                // end-of-file line changes the file operation (no more s2 structured data can follow, but user-defined data might)
                fileOperation = FileOperation.op_loadExtra;

                // pass the end-of-file message to all callbacks
        		for(ReadLineCallbackInterface callback : callbacks)
        			ret &= callback.onEndOfFile();
            } catch (Exception e) {
    			addErrorNote("Error while processing end-of-file");
            	ret = false;
    		}

            return ret;
        }

        /**
         * Process an unmarked end of file (that is, no more lines can be read but the end-of-file line is not present)
         *
         * @return union (and operator) of returns from all callbacks
         */
        boolean processUnmarkedEndOfFile() {
            boolean ret = true;

            try {
                // unmarked end-of-file means there is no more data to read, but the end-of-file mark was not encountered; the file operation should reflect the change
                fileOperation = FileOperation.op_none;

                // pass the end-of-file message to all callbacks
                for(ReadLineCallbackInterface callback : callbacks)
                    ret &= callback.onUnmarkedEndOfFile();
            } catch (Exception e) {
                addErrorNote("Error while processing end-of-file");
                ret = false;
            }

            return ret;
        }

        // helper for debugging "negative" timestamps in the PCARD produced s2 files; set to < 0 to disable
        // debugging so far - the timestamps in question seem to be produced by some MobECG development version
        public long countNegativeTsPcard = -1;

        /**
         * Process a data line - this is the main part of the library, the actual data that is stored in file
         *
         * @param handle    the data entity handle
         * @param len       length of the buffer
         * @param buffer    raw byte buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processDataLine(byte handle, int len, byte buffer []) {
        	boolean ret = true;        // ok by default

        	try {
                // TODO: decide how to decode timestamp and the following data based on how it was defined (through definition lines), instead of hard-coding it
                //   cachedHandles holds all the required data (assuming the definitions have been read successfully)
        		
        		TimestampDefinition td = cachedHandles[handle].timestampDefinition;
        		if (td == null)
                    throw new RuntimeException("Timestamp definition isn't declared for handle "+(int)handle+" ["+(handle > 32 ? (char)(handle) : '?')+"]");
        		        		
                long timestampRead = 0; // timestamp is max 8 bytes long
        		for(int i = 0; i < td.byteSize; i++)
        			timestampRead += (0x00FF & (int)buffer[i]) << ((i)*8L);

                //System.err.println((int)buffer[0]+"."+(int)buffer[1]+"."+(int)buffer[2]+" -| "+readingLineNum);
                if ((countNegativeTsPcard >= 0) && (timestampRead > 16600000)) {
                    countNegativeTsPcard++;
                    if (countNegativeTsPcard < 2)
                        System.err.println("  error line  " + readingLineNum + " |- " + timestampRead + " : " +
                            String.format("0x%08x", (0x0FF & (int)buffer[0])+(((int)buffer[1]<<8) & 0xFF00)+(((int)buffer[2]<<16) & 0xFF0000)) + " -> " +
                            (16777216 - timestampRead)
                    );
                }

                // if circumventing the timestamp bug of the old MobECGs, copy the explicitly set timestamp to definitions here
                if ((circumventTimestampBugOnRead) && (readingLineNum == (lastTimestampLineNum+1)))
                    copyTimestampToDefinitions();

    			if (specifiesRelativeValue(td.absoluteId))
    				cachedHandles[handle].lastAbsTimestamp.value += timestampRead*td.getNanoMultiplier();
    			else if (specifiesAbsoluteValue(td.absoluteId))
    				cachedHandles[handle].lastAbsTimestamp.value = timestampRead*td.getNanoMultiplier();
    			else // this should never happen though, if it was checked when loading timestamp definition
    				addErrorNote("Absolute id of timestamp definition for handle " + (int)handle + " is invalid");

                if ((messageMask & MESSAGE_TYPE.streamPacket.mask) > 0)
    			    for (ReadLineCallbackInterface callback : callbacks)
        	            ret &= callback.onStreamPacket(handle, cachedHandles[handle].lastAbsTimestamp.value, len-td.byteSize, Arrays.copyOfRange(buffer, td.byteSize, len));
    		} catch (Exception e) {
    			addErrorNote("Error while processing dataLine");
            	ret = false;
    		}

            return ret;
        }

        /**
         * Pass an error to the callbacks; TODO currently it is unused
         * @param err       length of the buffer
         * @return union (and operator) of returns from all callbacks
         */
        boolean processError(String err) {
            boolean ret = true;
            // add the error as a note to the S2 instance
            addErrorNote(err);

            // process error callbacks
            try {
                for (ReadLineCallbackInterface callback : callbacks)
                    ret &= callback.onError((int)readingLineNum, err);
            } catch (Exception e) {
                addErrorNote("Error while processing error (isn't it ironic?)");
                ret = false;
            }
            return ret;
        }

        /**
         * Copy the last received absolute timestamp to sensor definitions
         */
        void copyTimestampToDefinitions() {
            for (DataEntityCache c : cachedHandles)
                if (c != null)
                    c.lastAbsTimestamp = new Nanoseconds(lastTimestamp);
        }

        class RecordingSoftware {
            public String software;
            public int major, minor, revision;
            public String other;

            void parse(String value) {
                String[] words = value.trim().split("\\s+", 2);      // split on all whitespace
                if (words.length > 0) {
                    software = words[0];
                    major = minor = revision = 0;
                    other = "";
                    if (words.length > 1) {
                        try {
                            String[] vers = words[1].split("\\.", 3);
                            // warning, exception might be frown from the following three sentences (number format mismatch)
                            major = vers.length > 0 ? Integer.parseInt(vers[0]) : 0;
                            minor = vers.length > 1 ? Integer.parseInt(vers[1]) : 0;
                            if (vers.length > 2) {
                                String[] others = vers[2].split("[^\\d]", 2); // split to two parts on first non-numeric character
                                revision = others.length > 0 ? Integer.parseInt(others[0]) : 0;
                                other = others.length > 1 ? others[1] : "";
                            }
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                }
            }

            boolean versionBelow(int mj, int mi, int re) {
                return (major < mj) || (
                        (major == mj) && (
                                (minor < mi) || (
                                        (minor == mi) && (revision < re)
                                )
                        )
                );
            }
        }
    }

    // all registered entity handles are cached (data entity = sensor datum or a structure comprising data entities)
    public class DataEntityCache {
        String name = "";
        public String elementsInOrder = "";
        int bitSize = 0;
        Nanoseconds lastAbsTimestamp = new Nanoseconds(0);  // initialized to simplify and speed up processing
        TimestampDefinition timestampDefinition = null;
        public SensorDefinition sensorDefinition = null;
        StructDefinition structDefinition = null;

        void copyStructDefinition(StructDefinition sd) {
            structDefinition = sd;
            name = sd.name;
            elementsInOrder = sd.elementsInOrder;
            this.bitSize = calculateBits(elementsInOrder);
        }

        void copySensorDefinition(SensorDefinition sd) {
            name = sd.name;
            elementsInOrder = "";
            bitSize = totalBitSize(sd);
            sensorDefinition = sd;
        }

        void copyTimestampDefinition(TimestampDefinition td, Nanoseconds absTime) {
            timestampDefinition = td;
            lastAbsTimestamp = absTime;
        }

        /**
         * Test whether this handle can be used as a streaming data handle (it has all the required data vields registered)
         * @return true if this entity can be a data stream handle
         */
        public boolean isValidStreamHandle() {
            return (sensorDefinition != null) && (timestampDefinition != null);
        }

        public boolean isDataStream() {
            // TODO: this is not defined correctly probably should be (timestampDefinition != null)
            return sensorDefinition != null;
        }
    };
    DataEntityCache cachedHandles[];
    
    public DataEntityCache getEntityHandles(byte handle) {
    	return cachedHandles[handle];
    }

    public S2() {
        // static initializer is here because of the bug, which made the lines initializer execute before the LineType initializer.
        if (lines == null) {
            try {
            	lines = new HashMap<LineType, String>();
                //System.out.print((lineTypesArray[(byte) '#'] == null ? "# is null" : "# is not null") + "\n");
                lines.put(LineType.message, "A message in text form, encoded as UTF-8.");
                lines.put(LineType.specialMessage, "A special message in text form, where 2-char long identifier follows. First char of the identifier can be " +
                        "either 'w' (warning), 'e' (error), 'x' (exception), 'd' (debug), 'a' annotation, or 'n' (notification). Second char of the" +
                        " identification is either 1, 2, or 3, which stands for message coming from measuring device, the recording device, or the " +
                        "editing software, respectively.");
                lines.put(LineType.metadata, "Metadata in text form: 'key = value', where the only limitation is that the key must not contain '=' character.");
                lines.put(LineType.definition, "Definition of an entity. A single char handle and the single type definition type follow.");
                lines.put(LineType.version, "Version in text form. Space separates the format version and the extension version");
                lines.put(LineType.timestamp, "Timestamp (time passed from the measurement start) [ns]. It is to be used for setting up an anchor for all " +
                        "messages that use relative time (dt).");
                lines.put(LineType.endOfFile, "End of file.");
                lines.put(LineType.invalid, "Invalid line.");
            } catch (Throwable t) {
                System.out.print("caught throwable\n");
                t.printStackTrace(System.out);
            }
        }

        fileOperation = FileOperation.op_none;
        cachedHandles = new DataEntityCache[256];
    }

    /**
     * @brief Start file storage in the provided file (load can no longer be started on this file)
     * @param fname the filename (including path)
     * @return @link #StorageStatus object, which must be then used to access store-related functionality
     */
    public StoreStatus store(File directory, String fname) {
        if (fileOperation == FileOperation.op_none) {
            filename = new File(directory, fname);
            fileOperation = FileOperation.op_store;
            storeStatus = new StoreStatus();
            return storeStatus;
        } else {
            if (filename == null)
                addErrorNote("S2.store was called after the file has already been assigned to non-storage operation");
            else
                addErrorNote("S2.store was called again, first time file="+filename.toString()+", second time file="+new File(directory,
                        fname).toString());
            return null;
        }
    }

    /**
     * Start loading from the provided file
     * @param fname the filename (including path)
     * @return @link #LoadStatus object, which should be accessed to access load-related functionality
     */
    public LoadStatus load(File directory, String fname) {
        if (fileOperation == FileOperation.op_none) {
            filename = new File(directory, fname);
            fileOperation = FileOperation.op_load;
            loadStatus = new LoadStatus();
            return loadStatus;
        } else {
            if (filename == null)
                addErrorNote("S2.load was called after the file has already been assigned to non-load operation");
            else
                addErrorNote("S2.load was called again, first time file="+filename.toString()+", second time file="+new File(directory,
                        fname).toString());
            return null;
        }
    }

    /**
     * Get the LoadStatus if load was called before.
     * @return the load status object (provided load function has been called before)
     */
    public LoadStatus getLoadStatus() {
        return loadStatus;
    }

    /**
     * Calculates the number of bits required for storage of the provided structure
     * @param elementsInOrder The input elements (handles) of the struct
     * @return the number of bits required to store the provided struct elements
     */
    int calculateBits(String elementsInOrder) {
        int sum = 0;
        for (int i = 0; i < elementsInOrder.length(); ++i) {
            DataEntityCache it = cachedHandles[(byte) elementsInOrder.charAt(i)];

            if (it != null) {
                if (it.bitSize > 0) {
                    sum += it.bitSize;
                } else {
                    int bitSize = calculateBits(it.elementsInOrder);
                    sum += bitSize;
                    if (bitSize == 0)
                        throw new RuntimeException("Error: entity with 0 size encountered when processing elements: "+(int)elementsInOrder.charAt(i));
                }
            } else
                throw new RuntimeException("Error: unknown entity encountered when processing elements: " + (int)elementsInOrder.charAt(i));
        }
        return sum;
    }

    boolean isPossibleSensorHandle(byte handle)  {
        return handle < 32;
    }

    boolean isSensorHandle(byte handle)  {
        return (cachedHandles[handle] != null);
    }

    boolean isValidLine(byte type)  {
        return (lines.get(LineType.convert(type)) != null) || isSensorHandle(type);
    }

    static final boolean DEBUG_READ_LINE = false;

    //private long timestamp = 0; // Global variable timestamp for adding new timestamp values to current timestamp value.
    
    public DataEntityCache[] getCachedHandles()
    {
    	return cachedHandles;
    }
}
