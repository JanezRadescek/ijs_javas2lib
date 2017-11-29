package s2;

import java.util.*;

import s2.S2.DeviceType;
import s2.S2.EntityCache;
import s2.S2.MessageType;
import s2.S2.ReadLineCallbackInterface;
import s2.S2.SensorDefinition;
import s2.S2.StructDefinition;
import s2.S2.TimestampDefinition;


/*

    TODO: simplify entries (extended classes of MeasurementData.Entry); e.g. Version does not need be a class, just reuse S2.Version

 */


/**
 * This class can be passed to LoadStatus as the interface for callbacks and is able to store everything that is read from s2 file.
 * @author Rok Ivančič
 * @version 1.0.0
 */
public class MeasurementData implements ReadLineCallbackInterface{
    // increase this on every major update (mostly when features change)
    public int CLASS_VERSION = 2;
    // enable data mapping as specified in the s2 sensor description; if false, data will be left in raw form
	public boolean dataMapping = false;
    // used in the callbacks: define the interval for which the loading will be storing data (endpoints included)
    private long loadStartNanos = 0, loadEndNanos = Long.MAX_VALUE;

    /**
     * Base class for storing the s2 'line's which are being read
     */
	interface Entry {
		public void addLine(S2.StoreStatus s);
	}
	
	private S2 s2;
	
	/**
	 * This constructs a measurementData class with S2 file parameter which specifies data file
	 * @param file Raw data input
	 */
	public MeasurementData(S2 file) {
		s2 = file;
	}

	/**
	 * This class implements a simple comment
	 * @author rok
	 *
	 */
	public class Comment implements Entry {
		private long timestamp;
		private String comment;
		
		/**
		 * This constructs a comment with a specified timestamp and comment
		 * @param timestamp the timestamp of the comment
		 * @param comment the content of the comment
		 */
		public Comment(long timestamp, String comment)
		{
			this.timestamp = timestamp;
			this.comment = comment;
		}
		
		/**
		 * This returns the timestamp of comment in nanoseconds
		 * @return this comment's timestamp
		 */
		public long getTimestamp()
		{
			return this.timestamp;
		}
		
		/**
		 * This returns comment's string value
		 * @return this comment's value
		 */
		public String getComment()
		{
			return this.comment;
		}
		
		public void addLine(S2.StoreStatus s)
		{
			s.addTextMessage(comment);
		}
	}
	
	/**
	 * This class implements a simple extendedVersion
	 * @author rok
	 *
	 */
	public class Version implements Entry {
		private String baseVersion;
		private String extendedVersion;
		private int intVersion;
		
		/**
		 * This constructs a extendedVersion with int and string type of extendedVersion
		 * @param intVersion the int extendedVersion of the extendedVersion
		 * @param extendedVersion the string extendedVersion of the extendedVersion
		 */
		public Version(String baseVersion, String extendedVersion, int intVersion)
		{
			this.baseVersion = baseVersion;
			this.extendedVersion = extendedVersion;
			this.intVersion = intVersion;
		}

		/**
		 * This returns int type of extendedVersion
		 * @return this int extendedVersion
		 */
		public String getBaseVersion()
		{
			return baseVersion;
		}
		
		/**
		 * This returns string type of extendedVersion
		 * @return this string type of extendedVersion
		 */
		public String getExtendedVersion()
		{
			return extendedVersion;
		}

        /**
         * Return the extendedVersion as integer number (this is what is actually written in the file)
         * @return
         */
		public int getVersionIndex()
		{
			return intVersion;
		}
		
		public void addLine(S2.StoreStatus s)
		{
			//Ni addVersion v stor funkcijah
		}
	}

	/**
	 * This class implements a special message
	 * @author rok
	 *
	 */
	public class SpecialMessage implements Entry {
		private long timestamp; //ns
		private char who;
		private char what;
		private String message;
		private DeviceType dt;
		private MessageType mt;
		
		/**
		 * This constructs a special message with specified timestamp, who, what and message
		 * @param timestamp the timestamp of the special message of ns
		 * @param who the special message sender
		 * @param what the type of message
		 * @param message the content of message
		 */
		public SpecialMessage(long timestamp, char who, char what, String message)
		{
			this.timestamp = timestamp;
			this.who = who;
			this.what = what;
			this.message = message;
			this.dt = DeviceType.convert((byte)who);
			this.mt = MessageType.convert((byte)what);
		}
		
		/**
		 * This returns the timestamp of special message
		 * @return this special message's timestamp
		 */
		public long getTimestamp()
		{
			return this.timestamp;
		}
		
		/**
		 * This returns the sender of special message
		 * @return this special message's sender
		 */
		public char getWho()
		{
			return this.who;
		}
		
		/**
		 * This returns the type of special message
		 * @return this special message's type
		 */
		public char getWhat()
		{
			return this.what;
		}
		
		/**
		 * This returns the content of special message
		 * @return this special message's content
		 */
		public String getMessage()
		{
			return this.message;
		}
		
		/**
		 * This returns human readable sender value
		 * @return this special message's human readable sender
		 */
		public DeviceType getDeviceType()
		{
			return this.dt;
		}
		
		/**
		 * This returns human readable type value
		 * @return this special message's human readable message type
		 */
		public MessageType getMessageType()
		{
			return this.mt;
		}
		
		public void addLine(S2.StoreStatus s)
		{
			s.addSpecialTextMessage(dt, mt, message);
		}
	}

	/**
	 * This class implements metadata
	 * @author rok
	 *
	 */
	public class Metadata implements Entry {
		private String key;
		private String value;
		
		/**
		 * This constructs a metadata with a specified key and value
		 * @param key the key of metadata
		 * @param value the value of metadata
		 */
		public Metadata(String key, String value)
		{
			this.key = key;
			this.value = value;
		}
		
		/**
		 * This returns key of metadata
		 * @return this metadata's key
		 */
		public String getKey()
		{
			return this.key;
		}
		
		/**
		 * This returns value of metadata
		 * @return this metadata's value
		 */
		public String getValue()
		{
			return this.value;
		}
		
		public void addLine(S2.StoreStatus s)
		{
			s.addMetadata(key, value);
		}
	}
	
	/**
	 * This class implements PCARD stream packet
	 * @author rok
	 *
	 */
	public class PcardStreamPacket implements Entry {
		private byte handle;
		private long rawTimestamp; // ns
		private int counter;
		private float[] data;
		
		/**
		 * This constructs a stream packet with a specified handle, raw timestamp, counter and data
		 * @param handle the handle of the stram packet
		 * @param rawTimestamp the raw timestamp of the stram packet
		 * @param counter the counter of the stream packet
		 * @param data the array data of the stream packet
		 */
		public PcardStreamPacket(byte handle, long rawTimestamp, int counter, float[] data)
		{
			this.handle = handle;
			this.rawTimestamp = rawTimestamp;
			this.counter = counter;
			this.data = data;
		}

		/**
		 * This returns a handle of the stream packet
		 * @return this stream packet's handle
		 */
		public byte getHandle()
		{
			return this.handle;
		}

		/**
		 * This returns a raw timestamp of the stream packet
		 * @return this stream packet's raw timestamp
		 */
		public long getRawTimestamp()
		{
			return this.rawTimestamp;
		}
		
		/**
		 * This returns a counter of the stream packet
		 * @return this stream packet's counter
		 */
		public int getCounter()
		{
			return this.counter;
		}
		
		/**
		 * This returns array data of the stream packet
		 * @return this stream packet's array data
		 */
		public float[] getData()
		{
			return this.data;
		}
		
		public void addLine(S2.StoreStatus s)
		{
			byte[] b = new byte[19];
			MultiBitBuffer mbb = new MultiBitBuffer(b);
			//zaenkrat 14, potem moraš pogledati v cachedHandles
			SensorDefinition sd = null;
			StructDefinition stc = null;
			TimestampDefinition td = null;
			String elementsInOrder = "";
			long lastT = 0;
			for (EntityCache ec : s.getCachedHandles()) {
				if (ec == null)
					continue;

				if (ec.sensorDefinition != null)
					sd = ec.sensorDefinition; 
				if (ec.timestampDefinition != null) {
					td = ec.timestampDefinition;
//					if(ec.lastAbsTimestamp != null)
//						System.out.println("LDKAJLASKDJASKLDJ: " + ec.lastAbsTimestamp.count() + " ...... mulitplier: " + td.multiplier); lastT =ec.lastAbsTimestamp.count(); ec.lastAbsTimestamp = new S2.Nanoseconds(rawTimestamp);
				}
				if (ec.elementsInOrder.length() != 0)
					elementsInOrder = ec.elementsInOrder;
			}
				
			
			for(int i = 0; i < elementsInOrder.length()-1; i++)
			{
				float temp = (this.data[i] - sd.n) / sd.k;
				int toInt = Math.round(temp);
				mbb.setInts(toInt, i*sd.resolution, sd.resolution, 1);
			}
			int counterToInt = Math.round(this.data[elementsInOrder.length() - 1]);
			mbb.setInts(counterToInt, (elementsInOrder.length() - 1)*sd.resolution, sd.resolution, 1);
			System.out.println(String.format("[Handle: %s] [rawTimestamp: %s] [data: %s], [counter: %s]", handle, rawTimestamp, Arrays.toString(b), counterToInt));
			//TODO unkoment next line
			//System.out.println("*====================================*: " + ((rawTimestamp - lastT)/1000));
			//s.addSensorPacket(handle, (rawTimestamp - lastT)/1000, b); 	//TODO namesto te metode, moram pravilno pretvorit timestamp z metodo spodaj

			s.addStreamPacketRelative(handle, rawTimestamp, b, s);
		}
	}

	/**
	 * This class implements unknown stream packet
	 * @author rok
	 *
	 */
	public class UnknownStreamPacket implements Entry
	{
		private byte handle;
		private long rawTimestamp;
		private int counter;
		private float[] data;
		
		/**
		 * This constructs a unknown stream packet with a specified handle, raw timestamp, counter and data
		 * @param handle the handle of the stram packet
		 * @param rawTimestamp the raw timestamp of the stram packet
		 * @param counter the counter of the stream packet
		 * @param data the array data of the stream packet
		 */
		public UnknownStreamPacket(byte handle, long rawTimestamp, int counter, float[] data)
		{
			this.handle = handle;
			this.rawTimestamp = rawTimestamp;
			this.counter = counter;
			this.data = data;
		}
		
		/**
		 * This returns a handle of the stream packet
		 * @return this stream packet's handle
		 */
		public byte getHandle()
		{
			return this.handle;
		}
		
		/**
		 * This returns a raw timestamp of the stream packet
		 * @return this stream packet's raw timestamp
		 */
		public long getRawTimestamp()
		{
			return this.rawTimestamp;
		}
		
		/**
		 * This returns a counter of the stream packet
		 * @return this stream packet's counter
		 */
		public int getCounter()
		{
			return this.counter;
		}
		
		/**
		 * This returns array data of the stream packet
		 * @return this stream packet's array data
		 */
		public float[] getData()
		{
			return this.data;
		}
		
		public void addLine(S2.StoreStatus s)
		{
			s.addSensorPacket(handle, rawTimestamp, toByteArray(data));
		}
		
		private byte[] toByteArray(float[] data)
		{
			byte[] byteData = new byte[data.length];
			for(int i = 0; i < data.length; i++)
			{
				byteData[i] = (byte)data[i];
			}
			return byteData;
		}
	}
	
	/**
	 * This class implements error
	 * @author rok
	 *
	 */
	public class Error		//implements ni ??
	{
		private int lineNum;
		private String error;
		private long timestamp;
		
		/**
		 * This constructs an error with specified line number, error and timestmap
		 * @param lineNum the line number of error
		 * @param error the content of error
		 * @param timestamp the timestamp of error
		 */
		public Error(int lineNum, String error, long timestamp)
		{
			this.lineNum = lineNum;
			this.error = error;
			this.timestamp = timestamp;
		}
		
		/**
		 * This returns a line number of error
		 * @return this error's line number
		 */
		public int getLineNum()
		{
			return this.lineNum;
		}
		
		/**
		 * This return content of error
		 * @return this error's content
		 */
		public String getError()
		{
			return this.error;
		}
		
		/**
		 * This returns timestamp of error
		 * @return this error's timestamp
		 */
		public long getTimestamp()
		{
			return this.timestamp;
		}
	}
		
	public ArrayList<Comment> commentArray = new ArrayList<>();
	public Version version;
	public ArrayList<SpecialMessage> specialMessageArray = new ArrayList<>();
	public ArrayList<Metadata> metadataArray = new ArrayList<>();
	public Map<Byte, SensorDefinition> sensorDefinition = new HashMap<Byte, SensorDefinition>();
	public Map<Byte, StructDefinition> structDefinition = new HashMap<Byte, StructDefinition>(); 
	public Map<Byte, TimestampDefinition> timestampDefinition = new HashMap<Byte, TimestampDefinition>();
	public ArrayList<Long> timestampArray = new ArrayList<>();
	public Map<Byte, ArrayList<PcardStreamPacket>> streamPacketArrays = new HashMap<Byte, ArrayList<PcardStreamPacket>>();
	public Map<Byte, ArrayList<UnknownStreamPacket>> unknownStreamPacketArray = new HashMap<Byte, ArrayList<UnknownStreamPacket>>();
	public ArrayList<Error> errorArray = new ArrayList<>();
	// the last read timestamp is stored here; variable is initialized to 0 to simplify its usage
	private long lastExplicitTimestamp = 0;
    // the last read timestamp from streams or all other sources is stored here; variable is initialized to 0 to simplify its usage
    private long lastTimestamp = 0;

    /**
     * Returns the extendedVersion of the file.
     * @return
     */
    public Version getVersion() {
        return version;
    }

	/**
	 * This returns array list of comments
	 * @return this comment's array list
	 */
	public ArrayList<Comment> getComments()
	{
		return commentArray;
	}

	/**
	 * This returns array list of special messages
	 * @return this special message's array list
	 */
	public ArrayList<SpecialMessage> getSpecialMessages()
	{
		return specialMessageArray;
	}
	
	/**
	 * This returns array list of metadatas
	 * @return this metadata's array list
	 */
	public ArrayList<Metadata> getMetadata()
	{
		return metadataArray;
	}

	/**
	 * This returns Map of sensor definitions
	 * @return this sensor definition's Map
	 */
	public Map<Byte, SensorDefinition> getSensorDefinitions()
	{
		return sensorDefinition;
	}
	
	/**
	 * This returns Map of struct definitions
	 * @return this struct definition's Map
	 */
	public Map<Byte, StructDefinition> getStructDefinitions()
	{
		return structDefinition;
	}
	
	/**
	 * This returns Map of timestamp definitions
	 * @return this timestamp definition's Map
	 */
	public Map<Byte, TimestampDefinition> getTimestampDefinitions()
	{
		return timestampDefinition;
	}
	
	/**
	 * This returns array list of timestamps
	 * @return this timestamp's array list
	 */
	public ArrayList<Long> getTimestamps()
	{
		return timestampArray;
	}

	/**
	 * Return the last encountered timestamp, either read through explicit timestamp definition or by a stream packet
	 * @return
	 */
	public long getLastReadNanoTimestamp() {
		return lastTimestamp;
	}

	public int[] getAvailableStreams() {
        if (streamPacketArrays != null) {
            Set<Byte> s = streamPacketArrays.keySet();
            int[] a = new int[s.size()];
            int i = 0;
            for (Byte val : s)
                a[i++] = val;
            return a;
        } else {
            return new int[0];
        }
    }

	/**
	 * This return Map of stream packets
	 * @return this stream packet's Map
	 */
	public Map<Byte, ArrayList<PcardStreamPacket>> getStreamPackets() {
		return streamPacketArrays;
	}

    /**
     * This function clears all received stream packets; useful when loading file in multiple chunks
     */
	public void clearStreamPackets() {
        for (ArrayList<PcardStreamPacket> stream : streamPacketArrays.values()) {
            stream.clear();
        }
    }

    /**
     * This function is for octave/matlab interface
     * @param streamIndex the index of the stream to be converted fo a double array
     * @return all the data as double array (timestamp, counter, 14 samples, _repeat_)
     */
	public double[] getStreamArrayForMatlab(int streamIndex) {
        if (streamPacketArrays.get((byte)streamIndex) != null) {
            ArrayList<PcardStreamPacket> list = streamPacketArrays.get((byte) streamIndex);
            final int packetSize = (1+1+14);
            double[] bigArray = new double[list.size() * packetSize];
            for (int i = 0; i < list.size(); ++i) {
                PcardStreamPacket packet = list.get(i);
                bigArray[i*packetSize + 0] = packet.getRawTimestamp();
                bigArray[i*packetSize + 1] = packet.getCounter();
                for (int j = 0; j < packet.getData().length; ++j) {
                    bigArray[i*packetSize + 2 + j] = packet.getData()[j];
                }
            }
            return bigArray;
        } else
            return null;
    }

    /**
     * This function is for octave/matlab interface
     * @param streamIndex the index of the stream to be converted fo a double array
     * @return samples as float array
     */
    public float[] getStreamSamplesForMatlab(int streamIndex) {
        if (streamPacketArrays.get((byte)streamIndex) != null) {
            ArrayList<PcardStreamPacket> list = streamPacketArrays.get((byte) streamIndex);
            final int packetSize = 14;
            float[] bigArray = new float[list.size() * packetSize];
            for (int i = 0; i < list.size(); ++i)
                System.arraycopy(list.get(i).getData(), 0, bigArray, i*packetSize, packetSize);
            return bigArray;
        } else
            return null;
    }

    /**
     * This function is for octave/matlab interface
     * @param streamIndex the index of the stream for its timestamps to be extracted
     * @return all the timestamps as a long array
     */
    public long[] getStreamTimestampsForMatlab(int streamIndex) {
        if (streamPacketArrays.get((byte)streamIndex) != null) {
            ArrayList<PcardStreamPacket> list = streamPacketArrays.get((byte) streamIndex);
            long[] bigArray = new long[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                bigArray[i] = list.get(i).getRawTimestamp();
            }
            return bigArray;
        } else
            return null;
    }

    /**
     * This function is for octave/matlab interface
     * @param streamIndex the index of the stream for its sample counters to be extracted
     * @return all the counters as a long array
     */
    public long[] getStreamCountersForMatlab(int streamIndex) {
        if (streamPacketArrays.get((byte)streamIndex) != null) {
            ArrayList<PcardStreamPacket> list = streamPacketArrays.get((byte) streamIndex);
            long[] bigArray = new long[list.size()];
            for (int i = 0; i < list.size(); ++i) {
                bigArray[i] = list.get(i).getCounter();
            }
            return bigArray;
        } else
            return null;
    }

    /**
     * Returns the indexed stream
     * @return the stream as ArrayList of PcardStreamPacket
     */
    public ArrayList<PcardStreamPacket> getStreamPackets(int streamIndex) {
        return streamPacketArrays.get((byte)streamIndex);
    }

    /**
	 * This return Map of unknown stream packets
	 * @return this unknown stream packet's Map
	 */
	public Map<Byte, ArrayList<UnknownStreamPacket>> getUnknownLines()
	{
		return unknownStreamPacketArray;
	}
	
	/**
	 * This returns array list of errors
	 * @return this error's array list
	 */
	public ArrayList<Error> getErrors() {
		return errorArray;
	}


	//region ReadLineCallbackInterface implementation

    /**
     * Set the time interval from which the load will take place; all data and other time-based lines will be loaded only if it appears within this interval
     * @param start start of the time interval (included)
     * @param end   end of the interval (included)
     */
    public void setTimeIntervalInNanos(long start, long end) {
        loadStartNanos = Math.max(0, Math.min(Long.MAX_VALUE, start));
        loadEndNanos = Math.max(0, Math.min(Long.MAX_VALUE, end));
    }

    /**
     * Set the time interval from which the load will take place; all data and other time-based lines will be loaded only if it appears within this interval
     * @param start start of the time interval (included)
     * @param end   end of the interval (included)
     */
    public void setTimeInterval(S2.Nanoseconds start, S2.Nanoseconds end) {
        loadStartNanos = start.getValue();
        loadEndNanos = end.getValue();
    }

	@Override
	public boolean onComment(String comment) {
        if (lastTimestamp >= loadStartNanos)
		    commentArray.add(new Comment(lastTimestamp, comment));
		return true;
	}

	@Override
	public boolean onVersion(int versionInt, String extendedVersion) {
		version = new Version(S2.getStringVersion(versionInt), extendedVersion, versionInt);
    	return true;
    }

	@Override
	public boolean onSpecialMessage(char who, char what, String message) {
        //if (lastTimestamp >= loadStartNanos)
		    specialMessageArray.add(new SpecialMessage(lastTimestamp, who, what, message));
    	
		return true;
	}

	@Override
	public boolean onMetadata(String key, String value) {
    //commented out for easier tracking of changes during measurement
    //if (lastTimestamp >= loadStartNanos)
		metadataArray.add(new Metadata(key, value));
		return true;
	}

	@Override
	public boolean onEndOfFile()
	{
		return true;
	}

	@Override
	public boolean onUnmarkedEndOfFile()
	{
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, SensorDefinition definition) {
    	if (sensorDefinition.get(handle) == null)
    		sensorDefinition.put(handle, definition);
    	else
    		throw new RuntimeException("Handle already has a sensor definition");

		return true;
	}

	@Override
	public boolean onDefinition(byte handle, StructDefinition definition) {
		if (structDefinition.get(handle) == null)
			structDefinition.put(handle, definition);
		else
			throw new RuntimeException("Handle already has a struct definition");
		return true;
	}

	@Override
	public boolean onDefinition(byte handle, TimestampDefinition definition) {
		if (timestampDefinition.get(handle) == null)
			timestampDefinition.put(handle, definition);
		else
			throw new RuntimeException("Handle already has a timestamp definition");
		return true;
	}

	@Override
	public boolean onTimestamp(long nanoSecondTimestamp) {
        if (lastTimestamp >= loadStartNanos) {
            timestampArray.add(nanoSecondTimestamp);
        }
		lastExplicitTimestamp = nanoSecondTimestamp;
        lastTimestamp = nanoSecondTimestamp;
		return (lastTimestamp < loadEndNanos);
	}

	@Override
	public boolean onStreamPacket(byte handle, long timestamp, int len, byte data[]) {
        lastTimestamp = timestamp;
        if (lastTimestamp >= loadStartNanos) {
            ArrayList<Float> sensorData = new ArrayList<>();

            MultiBitBuffer mbb = new MultiBitBuffer(data);

            int mbbOffset = 0;
            int sampleCounter = 0;        // sample counter is still half-way hardcoded into stream reading

            for (int i = 0; i < s2.getEntityHandles(handle).elementsInOrder.length(); ++i) {
                byte cb = (byte) s2.getEntityHandles(handle).elementsInOrder.charAt(i);
                int entitySize = s2.getEntityHandles(cb).sensorDefinition.resolution;
                int temp = mbb.getInt(mbbOffset, entitySize);
                mbbOffset += entitySize;
                if (s2.getEntityHandles(cb).sensorDefinition != null) {
                    // hard-coded PCARD data
                    if (cb == 'e') {
                        if (dataMapping) {
                            // to convert from ADC integer to milli Volts:
                            sensorData.add(temp * s2.getEntityHandles(cb).sensorDefinition.k + s2.getEntityHandles(cb).sensorDefinition.n); //convert to specific unit (mV)
                        } else {
                            // to leave the ADC raw data:
                            sensorData.add((float) temp);
                        }
                    } else
                        sampleCounter = (int) (temp * s2.getEntityHandles(cb).sensorDefinition.k + s2.getEntityHandles(cb).sensorDefinition.n); //convert to specific unit (mV)
                } else {
                    System.out.println("Measurement data encountered invalid sensor: " + (int) (cb));
                }
            }

            float[] sData = new float[sensorData.size()];
            for (int i = 0; i < sData.length; i++)
                sData[i] = sensorData.get(i);    //Convert from ArrayList to array[]

            ArrayList<PcardStreamPacket> packetArray = streamPacketArrays.get(handle);
            if (packetArray == null) {
                packetArray = new ArrayList<PcardStreamPacket>();
                streamPacketArrays.put(handle, packetArray);
            }
            packetArray.add(new PcardStreamPacket(handle, timestamp, sampleCounter, sData));
        }
		return (lastTimestamp < loadEndNanos);
	}

	@Override
	public boolean onUnknownLineType(byte type, int len, byte data[]) {
        if (lastTimestamp >= loadStartNanos) {
            ArrayList<Character> handles = new ArrayList<>();
            for (char c : s2.getEntityHandles(type).elementsInOrder.toCharArray()) {
                handles.add(c);
            }

            ArrayList<Float> sensorData = new ArrayList<>();
            MultiBitBuffer mbb = new MultiBitBuffer(data);    // Getting 10-bit sample from 19-Bytes

            for (int i = 0; i < handles.size() - 1; i++) {
                int temp = mbb.getInt(i * s2.getEntityHandles((byte) handles.get(i).charValue()).sensorDefinition.resolution, s2.getEntityHandles((byte) handles.get(i).charValue()).sensorDefinition.resolution);
                sensorData.add(temp * s2.getEntityHandles((byte) handles.get(i).charValue()).sensorDefinition.k + s2.getEntityHandles((byte) handles.get(handles.size() - 1).charValue()).sensorDefinition.n); //convert to specific unit (mV)
            }
            int tenBitTime = mbb.getInt((handles.size() - 1) * (s2.getEntityHandles((byte) handles.get(handles.size() - 1).charValue()).sensorDefinition.resolution), s2.getEntityHandles((byte) handles.get(handles.size() - 1).charValue()).sensorDefinition.resolution); // 10-bit sample counter

            float[] sData = new float[sensorData.size()];
            for (int i = 0; i < sData.length; i++)
                sData[i] = sensorData.get(i);    //Convert from ArrayList to array[]

            if (unknownStreamPacketArray.get(type) == null)
                unknownStreamPacketArray.put(type, new ArrayList<UnknownStreamPacket>());

            unknownStreamPacketArray.get(type).add(new UnknownStreamPacket(type, lastTimestamp, tenBitTime, sData));
        }
		return true;
	}

	@Override
	public boolean onError(int lineNum,  String error) {
        if (lastTimestamp >= loadStartNanos)
		    errorArray.add(new Error(lineNum, error, lastTimestamp));
		return true;
	}
    //endregion
}
