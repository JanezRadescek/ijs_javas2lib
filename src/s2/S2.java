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

    private long messageMask;
    private String unit;

    public void setMessageMask(long messageMask) {
        this.messageMask = messageMask;
    }

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
        byte resolution;         // in bits per sample
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

    static int totalBitSize(SensorDefinition sensorDefinition) {
        return (sensorDefinition.vectorSize * (sensorDefinition.resolution + sensorDefinition.scalarBitPadding)) + sensorDefinition.vectorBitPadding;
    }

    public static class Nanoseconds {
        private long value;

        public Nanoseconds(long val) {
            value = val;
        }

        public long getValue() {
            return value;
        }
    }

    public static class TimestampDefinition {
        byte absoluteId;
        byte byteSize;
        double multiplier;     // multiply timestamp with this value to get seconds

        long getNanoMultiplier() { return (long)(multiplier*1e9 + 0.5); }
        /// transform the given timestamp to nanoseconds
        Nanoseconds toNanoSeconds(long stamp) { return new Nanoseconds(getNanoMultiplier() * stamp); }
        long toImplementationFormat(Nanoseconds nanoStamp) { return nanoStamp.getValue() / getNanoMultiplier(); }
        public TimestampDefinition(AbsoluteId absoluteId, byte byteSize, double multiplier) {
            this.absoluteId = absoluteId.byteId;
            this.byteSize = byteSize;
            this.multiplier = multiplier;
        }
    }

    public static class StructDefinition {
        public String name;
        public String elementsInOrder;

        public StructDefinition(String name, String elementsInOrder) {
            this.name = name;
            this.elementsInOrder = elementsInOrder;
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

        static MessageType convert(byte input) {
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
    // number of errors encountered while processing e3b requests (should be 0 or the file might not be loaded/stored correctly)
    int numErrors = 0;
    // number of errors encountered while processing e3b requests (should be 0 or the file might not be loaded/stored correctly)
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

    // time reset can be ordered with new value for 'last time' for all lines that use dt (difference in time); time is always reset at the beginning
    // of the measurement; value 0 causes the first timestamp taken to be set as 'lastTimeResetValue'.
    long lastTimeResetValue = 0;
    long absoluteTimers[] = new long[256]; // array elements are always 0 after the allocation

    // when reading lines, current linenumber should be accessible from here
    long readingLineNum;

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
     * Get the text notes (errors, warnings, ...) that were produced since the e3b was started up
     * @return the string containing notes separated by newlines
     */
    public String getNotes() {
        return notes;
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
        boolean onMetadata( String key,  String value);
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
    };

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
        
        public EntityCache[] getCachedHandles() {
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
         * Get the text notes (errors, warnings, ...) that were produced since the e3b was started up
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

            EntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new S2.EntityCache();
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

            EntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new S2.EntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copyStructDefinition(structDefinition, calculateBits(structDefinition.elementsInOrder));

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

            EntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new S2.EntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copyTimestampDefinition(timestampDefinition);

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
            EntityCache entity = cachedHandles[t];
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

            EntityCache entity = cachedHandles[t];
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
			try {
	           // store data to streaming data file:
				long lastTimestamp = 0;
				for(EntityCache ec : cachedHandles) {
					if(ec!= null && ec.timestampDefinition != null) {
						if(ec.lastAbsTimestamp != null) {
							lastTimestamp = ec.lastAbsTimestamp.getValue();
							ec.lastAbsTimestamp = new S2.Nanoseconds(rawTimestamp);
						}
					}
				}
				
	            // calculate time difference
	            long d = rawTimestamp - lastTimestamp;
	            long dFormatted = (long)(d / 1000); //(long)(d * multiplier);
	            if (dFormatted < 256*256*128) {
	                // timestamp diff can fit inside a normal message; offset is used here instead of the assignment (lastReceivedTime = receivedTime)
	                // to account for the rounding effects of storing less precise times 
	            	lastTimestamp = (Math.round(dFormatted/1000));
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
         * @param t
         * @param data
         */
        void writeLine(byte t, byte data) {
            byte buf[] = new byte[1];
            buf[0] = data;
            if (DEBUGWriteLine) System.out.print("WriteLine(byte t, byte data), byte=" + data + "\n");
            writeLine(t, buf);
        }

        /**
         * Write a line (type @link #t) to file with string @link #data as data
         * @param t
         * @param data
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
         * @param t
         * @param data
         */
        void writeLine(byte t, byte data[]) {
            if (data.length > 255)
                throw new RuntimeException("Error in S2.writeLine: data length = "+data.length+", when max data length is 255");
            writeLine(t, data, (byte)data.length, -1);
        }

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
                temp = new DeferredWriteBuffer(this, file.getFilePointer(), byteDataSize);
                // write a dummy line
                dataSize -= getLineOverhead();
                if (dataSize == 0) {
                    writeLine((byte) '#', "");
                } else {
                    byte dummyText[] = new byte[dataSize]; // filled with zeros, which is perfect
                    writeLine((byte) '#', dummyText);
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

    public class LoadStatus {
        BufferedInputStream file;

        // callbacks for processing lines when reading from s2 file
        private ArrayList<ReadLineCallbackInterface> callbacks = new ArrayList<ReadLineCallbackInterface>();

        LoadStatus() {
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
         * function that checks that the stream is valid to write to
         * @return true if the stream is ok
         */
        public boolean isOk() {
            return (file != null) && (fileOperation == FileOperation.op_load);
        }

        /**
         * @brief Read a single line from file and process it
         * @return
         */
        public boolean processLine(Line line) {
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
         * Read all lines and process them
         * return   false if an unrecoverable error occurs (file is not open, etc), true if everything is loaded smoothly (even if some lines are not recognised)
         */
        public boolean readAllLinesAndProcess() {
            // first, determine if the file was loaded successfully and terminate gracefully if not
            if (file == null) {
                addErrorNote("readAllLinesAndProcess failed, file was not loaded successfully");
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
         * @brief Use callbacks to parse information from lines that are properly fomratted, their types known, and also successfully read from input file
         */
        public void addReadLineCallback(ReadLineCallbackInterface callback) {
            callbacks.add(callback);
        }

        public EntityCache getEntityHandles(byte handle) {
            return cachedHandles[handle];
        }

        /**
         * @brief Remove all read line callbacks
         */
        public void clearReadLineCallbacks() {
            callbacks.clear();
        }

        /**
         * @brief Convert the supplied relative timestamp (usually called dt) to an absolute timestamp. Warning: must not call this function with out-of-order timestamps!
         * @param dt The time difference from the previous registered timestamp (either relative or absolute)
         * @return The 'absolute time', which is just time relative to the measurement start. All relative timestamps should be converted in the order that they appear in the file.
         */
        long relativeToAbsoluteTime(byte handle, long dt) {
            if (handle < absoluteTimers.length) {
                absoluteTimers[handle] += dt;
                return absoluteTimers[handle];
            } else
                return 0;
        }

        class Line {
            byte op;
            int len;
            byte[] data;
            boolean success;

            Line(byte op, int len, byte[] data) {
                this.op = op;
                this.len = (data == null ? 0 : (byte)data.length);
                success = (this.len == len);
                this.len = len;
                this.data = data;
            }

            Line() {
                success = false;
                data = null;
            }
        }

        Line readLine() {
            assert (fileOperation == FileOperation.op_load);
            assert (loadStatus != null);
            assert (maxLineBufferLength > 255);

            ++readingLineNum;
            // read the 2-char header, which is always present
            try {
                byte header[] = new byte[2];
                int numAvail = file.available();
                if (numAvail >= 2) {
                    int numRead = file.read(header);
                    if (numRead != 2) {
                        addErrorNote("Error in LoadStatus.readLine: could not read header of line; only " + numRead +
                                " bytes were read, although "+numAvail+" should be available.\n");
                        return new Line(LineType.invalid.byteId, numRead, (numRead > 0 ? Arrays.copyOf(header, numRead) : null));
                    }
                    // read data
                    int bufLength = ((int)header[1]) & 0xff;	// convert unsigned byte (length cannot be negative) to integer
                    byte buf[] = bufLength == 0 ? null : new byte[bufLength];
                    boolean readOk = (bufLength == 0) || (bufLength == file.read(buf));
                    if (readOk) {
                        // ignore the newline that follows the line
                        readOk = (1 == file.skip(1));
                        if (!readOk)
                            addErrorNote("Warning in LoadStatus.readLine: missing newline.\n");
                    } else {
                        addErrorNote("Error in LoadStatus.readLine: could not read full line (length="+(int)header[1]+" bytes.\n");
                    }
                    return new Line(header[0], bufLength, buf);
                } else {
                    if (numAvail == 0) {
                        addWarning("Warning while reading line (LoadStatus.readLine): end of file was reached but the end was not marked; this indicates that the file is incomplete - write procedure was interrupted before it finished.\n");
                        return null;
                    } else {
                        byte buf[] = new byte[numAvail];
                        int numRead = file.read(buf);

                        addErrorNote("Error in LoadStatus.readLine: could not read header of line " + readingLineNum + "; only "+numAvail+"bytes were available.\n");
                        return new Line(LineType.invalid.byteId, numRead, buf);
                    }
                }
            } catch (IOException e) {
                addErrorNote("Error in LoadStatus.readline: IOException caught.\n");
                return null;
            }
        }

        boolean processUnknownLine(byte op, int len, byte buffer[]) {
        	boolean ret = false;        // ok by default
        	String data;
        	try {
    			data = new String(buffer, "UTF-8");
    			for(ReadLineCallbackInterface callback : callbacks)
            		ret &= callback.onUnknownLineType(op, len, buffer);

    		} catch (Exception e) {
    			addWarning("Error while processing unrecognised line");
            	ret = false;
    		}

            return ret;
        }

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

        boolean processMetadata(int len, byte buffer []) {
            boolean ret = true;        // ok by default
            try {
        		String metadata = len == 0 ? "" : new String(buffer, "UTF-8");
        		String[] keyValue = metadata.split("=");
            	for(ReadLineCallbackInterface callback : callbacks)
            		ret &= callback.onMetadata(keyValue[0], keyValue[1]);
            } catch (Exception e) {
                addErrorNote("Error while processing metadata");
                ret = false;
            }

            return ret;
        }

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

        boolean processDefinitionLeaf(byte handle, int len, byte buffer[]) {
            boolean ret = true;
            StringBuffer nameSensor = new StringBuffer();
            StringBuffer unit = new StringBuffer();

            int bufferIndex = 2;
            int nameLengthSensor = buffer[2];
            for (int i = 0; i < nameLengthSensor; i++)	// Getting name
            {
                nameSensor.append((char)buffer[++bufferIndex]);
            }

            int unitLength = buffer[++bufferIndex];
            for (int i = 0; i < unitLength; i++)		// Getting unit
            {
                unit.append((char)buffer[++bufferIndex]);
            }

            byte resolutionByte = buffer[++bufferIndex];
            byte bitPaddingByte = buffer[++bufferIndex];
            byte valueTypeByte = buffer[++bufferIndex];
            byte absoluteIdByte = buffer[++bufferIndex];
            byte vectorSizeByte = buffer[++bufferIndex];
            byte vectorBitPaddingByte = buffer[++bufferIndex];

            //Get frequency
            byte[] frequencyByte = new byte[4];
            for (int i = 0; i < 4; i++)
                frequencyByte[i] = (byte)buffer[++bufferIndex];

            float frequencyValue = ByteBuffer.wrap(frequencyByte).order(ByteOrder.LITTLE_ENDIAN).getFloat();

            //Get k
            byte[] kByte = new byte[4];
            for (int i = 0; i < 4; i++)
                kByte[i] = (byte)buffer[++bufferIndex];

            float kValue = ByteBuffer.wrap(kByte).order(ByteOrder.LITTLE_ENDIAN).getFloat();

            //Get n
            byte[] nByte = new byte[4];
            for (int i = 0; i < 4; i++)
                nByte[i] = (byte)buffer[++bufferIndex];

            float nValue = ByteBuffer.wrap(nByte).order(ByteOrder.LITTLE_ENDIAN).getFloat();

            SensorDefinition sd = new SensorDefinition(nameSensor.toString());
            sd.unit = unit.toString();
            sd.resolution = resolutionByte;
            sd.scalarBitPadding = bitPaddingByte;
            sd.valueType = valueTypeByte;
            sd.absoluteId = absoluteIdByte;
            sd.vectorSize = vectorSizeByte;
            sd.vectorBitPadding = vectorBitPaddingByte;
            sd.samplingFrequency = frequencyValue;
            sd.k = kValue;
            sd.n = nValue;

            EntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new S2.EntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copySensorDefinition(sd);

            for(ReadLineCallbackInterface callback : callbacks)
                ret &= callback.onDefinition(buffer[0], sd);
            return ret;
        }

        boolean processDefinitionStruct(byte handle, int len, byte buffer[]) {
            boolean ret = true;
            StringBuffer nameStruct = new StringBuffer();
            StringBuffer elements = new StringBuffer();

            int bufferIndexSensor = 2;

            int nameLengthStruct = buffer[2];
            for (int i = 0; i < nameLengthStruct; i++)	// Getting name
                nameStruct.append((char)buffer[++bufferIndexSensor]);

            int elementLength = buffer[++bufferIndexSensor];
            for (int i = 0; i < elementLength; i++)		// Getting elements
                elements.append((char)buffer[++bufferIndexSensor]);

            StructDefinition sdef = new StructDefinition(nameStruct.toString(), elements.toString());
            EntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new S2.EntityCache();
                cachedHandles[handle] = entity;
            }
            int length = len;	// Converts byte to int
            entity.copyStructDefinition(sdef, length);

            for(ReadLineCallbackInterface callback : callbacks)
                ret &= callback.onDefinition(buffer[0], sdef);
            return ret;
        }

        boolean processDefinitionTimestamp(byte handle, int len, byte buffer[]) {
            boolean ret = true;
            int bufferIndexTs = 2;
            char absoluteIdChar = (char)buffer[bufferIndexTs];
            byte byteSizeValue = buffer[++bufferIndexTs];

            byte[] byteMultiplier = new byte[8];
            for(int i = 0; i < 8; i++)		// Getting multiplier
                byteMultiplier[i] = buffer[++bufferIndexTs];

            double resolutionTs = ByteBuffer.wrap(byteMultiplier).order(ByteOrder.LITTLE_ENDIAN).getDouble();

            AbsoluteId ai = AbsoluteId.convert((byte)absoluteIdChar);
            TimestampDefinition td = new TimestampDefinition(ai, byteSizeValue, resolutionTs);

            EntityCache entity = cachedHandles[handle];
            if (entity == null) {
                // add a new handle
                entity = new S2.EntityCache();
                cachedHandles[handle] = entity;
            }
            entity.copyTimestampDefinition(td);

            for(ReadLineCallbackInterface callback : callbacks)
                ret &= callback.onDefinition(buffer[0], td);
            return ret;
        }

        boolean processDefinition(int len, byte buffer []) {
        	boolean ret = true;           // ok by default
            byte handle = buffer[0];      // At position 0 is specified handle
            DefinitionType dt = DefinitionType.convert(buffer[1]); // Type definition is specified in buffer at position 1
        	try {
        		switch (dt)	{
        		case deftype_leaf:
                    ret = processDefinitionLeaf(handle, len, buffer);
        			break;
        		case deftype_struct:
                    ret = processDefinitionStruct(handle, len, buffer);
        			break;
        		case deftype_timestamp:
        			ret = processDefinitionTimestamp(handle, len, buffer);
        			break;
        		}
    		} catch (Exception e) {
    			addErrorNote("Error while processing definition");
            	ret = false;
    		}

            return ret;
        }

        boolean processTimestamp(int len, byte buffer []) {
            boolean ret = true;        // ok by default
            //TODO this timestamp does not have handle but correct all timestamps for each handle in this value
            try {
    			long timestampAbs = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).getLong();
    			
    			for(EntityCache c : cachedHandles)
                    if(c!=null)
    				    c.lastAbsTimestamp = new Nanoseconds(timestampAbs);
    			
    			//timestamp = timestampAbs;
    			for(ReadLineCallbackInterface callback : callbacks)
        			ret &= callback.onTimestamp(timestampAbs);
            } catch (Exception e) {
                addErrorNote("Error while processing timestamp");
                ret = false;
            }

            return ret;
        }

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
        		
    			if (specifiesRelativeValue(td.absoluteId))
    				cachedHandles[handle].lastAbsTimestamp.value += timestampRead*td.getNanoMultiplier();
    			else if (specifiesAbsoluteValue(td.absoluteId))
    				cachedHandles[handle].lastAbsTimestamp.value = timestampRead*td.getNanoMultiplier();
    			else // this should never happen though, if it was checked when loading timestamp definition
    				addErrorNote("Absolute id of timestamp definition for handle " + (int)handle + " is invalid");

                if((messageMask & MESSAGE_TYPE.streamPacket.mask) > 0)
    			    for (ReadLineCallbackInterface callback : callbacks)
        	            ret &= callback.onStreamPacket(handle, cachedHandles[handle].lastAbsTimestamp.value, len-td.byteSize, Arrays.copyOfRange(buffer, td.byteSize, len));
    		} catch (Exception e) {
    			addErrorNote("Error while processing dataLine");
            	ret = false;
    		}

            return ret;
        }

        boolean processError(String err) {
            boolean ret = true;

            try {
                for (ReadLineCallbackInterface callback : callbacks)
                    ret &= callback.onError((int)readingLineNum, err);
                return ret;
            } catch (Exception e) {
                addErrorNote("Error while processing error (isn't it ironic?)");
                ret = false;
            }
            return ret;
        }
    }

    // all registered entity handles are cached (entity = sensor or struct)
    public class EntityCache {
        String name = "";
        String elementsInOrder = "";
        int bitSize = 0;
        public Nanoseconds lastAbsTimestamp = null;
        public Nanoseconds lastRelativeTime = null;
        TimestampDefinition timestampDefinition = null;
        SensorDefinition sensorDefinition = null;
        StructDefinition structDefinition = null;

        void copyStructDefinition(StructDefinition sd, int bitSize) {
            structDefinition = sd;
            name = sd.name;
            elementsInOrder = sd.elementsInOrder;
            this.bitSize = bitSize;
        }

        void copySensorDefinition(SensorDefinition sd) {
            name = sd.name;
            elementsInOrder = "";
            bitSize = totalBitSize(sd);
            sensorDefinition = sd;
        }

        void copyTimestampDefinition(TimestampDefinition td) {
            timestampDefinition = td;
            lastAbsTimestamp = new Nanoseconds(0);
            lastRelativeTime = new Nanoseconds(0);
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
    EntityCache cachedHandles[];
    
    public EntityCache getEntityHandles(byte handle) {
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

        messageMask = Long.MAX_VALUE;
        fileOperation = FileOperation.op_none;
        cachedHandles = new EntityCache[256];
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
     * @brief Start loading from the provided file
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
     * @brief Get the LoadStatus if load was called before.
     * @return the load status object (provided load function has been called before)
     */
    public LoadStatus getLoadStatus() {
        return loadStatus;
    }

    /**
     * @brief Caluclates the number of bits required for storage of the provided structure
     * @param elementsInOrder The input elements (handles) of the struct
     * @return the number of bits required to store the provided struct elements
     */
    int calculateBits(String elementsInOrder) {
        int sum = 0;
        for (int i = 0; i < elementsInOrder.length(); ++i) {
            EntityCache it = cachedHandles[(byte) elementsInOrder.charAt(i)];

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

    /**
     * @brief Checks that the supplied time is within the bit limit provided by #numBits; also updates the last-reset-time if needed
     * @param t timestamp [ns]
     * @param numBits is the number of bits that are available - if (t - ref) takes more bits, then false will be returned
     * @return true if the time is within limits
     */
    boolean checkTime(Long ref, long t, int numBits) {
        if (lastTimeResetValue == 0) {
            lastTimeResetValue = t;
            ref = t;
        }

        return ((t - ref) & (0xFFFFFFFF << numBits)) == 0;
    }

    void timeReset() {
        lastTimeResetValue = 0;
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
    
    public EntityCache[] getCachedHandles()
    {
    	return cachedHandles;
    }
}
