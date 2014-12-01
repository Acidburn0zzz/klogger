/**
 * Copyright 2014 BlackBerry, Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package com.blackberry.logdriver.klogger;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackberry.krackle.producer.ProducerConfiguration;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for configuring KLogger.
 *
 * It extends ProducerConfiguration from Krackle, so any property that is valid there is also valid here.
 *
 * In addition, the following are possible.
 *
 * <table>
 * <tr>
 * <th>property</th>
 * <th>default</th>
 * <th>description</th>
 * </tr>
 *
 * <tr>
 * <td>client.id</td>
 * <td>the local hostname</td>
 * <td>The client ID to send with requests to the broker.</td>
 * </tr>
 *
 *
 *
 * <tr>
 * <td>kafka.key</td>
 * <td>the local hostname</td>
 * <td>The key to use when partitioning all data topics sent from this instance.
 * </td>
 * </tr>
 *
 * <tr>
 * <td>kafka.rotate</td>
 * <td>whether to RR through partitions</td>
 * <td>Whether or not we switch to the next available partitions on each Meta-Data Refresh or not (default=false)
 * </td>
 * </tr>
 *
 * <tr>
 * <td>tcp.receive.buffer.bytes</td>
 * <td>1024 * 1024</td>
 * <td>Suggested size of the TCP receive buffer.</td>
 * </tr>
 *
 * <tr>
 * <td>max.line.length</td>
 * <td>64 * 1024</td>
 * <td>Maximum line length accepted. Longer lines will be split into multiple lines of at most this size.</td>
 * </tr>
 *
 * <tr>
 * <td>encode.timestamp</td>
 * <td>true</td>
 * <td>Whether or not to encode the timestamp in front of the logline</td>
 * </tr>
 *
 * <tr>
 * <td>validate.utf8</td>
 * <td>true</td>
 * <td>If this is set to true, then all incoming log lines will be validated to ensure that they are correctly encoded in UTF-8. Invalid bytes will be replaced by the replacement character (U+FFFD).</td>
 * </tr>
 *
 * <tr>
 * <td>sources</td>
 * <td></td>
 * <td>(required) A comma separated list of source names which should be active.
 * </td>
 * </tr>
 *
 * <tr>
 * <td>source.<em>sourceName</em>.port</td>
 * <td></td>
 * <td>(required) One entry is required for each source listed in <code>sources</code>.
 * <p>
 * The port to listen on for messages send via this source.</td>
 * </tr>
 *
 * <tr>
 * <td>source.<em>sourceName</em>.topic</td>
 * <td></td>
 * <td>(required) One entry is required for each source listed in <code>sources</code>.
 * <p>
 * The Kafka topic that messages sent via this <code>source</code> should be sent to.</td>
 * </tr>
 *
 * <tr>
 * <td>source.<em>sourceName</em>.quick.rotate</td>
 * <td>(optional) This is optional for each source listed in <code>sources</code>
 * <p>
 * Whether or not we will do quickRotate for this Topic.</p>
 * </td>
 * </tr>
 *
 * <tr>
 * <td>source.<em>sourceName</em>.quick.rotate.msgblks</td>
 * <td>(optional) This is optional for each source listed in <code>sources</code>
 * <p>
 * Whether or not we will do quickRotate for this Topic.</p>
 * </td>
 * </tr>
 *
 *
 * </table>
 */
public class Configuration extends ProducerConfiguration
{
	private static final Logger LOG = LoggerFactory.getLogger(Configuration.class);

	//private List<PortSource> portSources;
	private ArrayList<PortSource> portSources = new ArrayList<>();
	private ArrayList<FileSource> fileSources = new ArrayList <>();	
	private String clientId;
	private String kafkaKey;
	private boolean rotatePartitions;
	private int tcpReceiveBufferBytes;
	private int maxLineLength;
	private boolean encodeTimestamp;
	private boolean validateUtf8;

	public Configuration(Properties props) throws Exception
	{
		super(props);

		if (props.containsKey("client.id"))
		{
			clientId = props.getProperty("client.id");
		} 
		else
		{
			// Try to figure out my local hostname.
			clientId = InetAddress.getLocalHost().getHostName();
		}
		
		LOG.info("client.id = {}", clientId);

		if (props.containsKey("kafka.key"))
		{
			kafkaKey = props.getProperty("kafka.key");
		} 
		else
		{
			// Try to figure out my local hostname.
			kafkaKey = InetAddress.getLocalHost().getHostName();
		}
		
		LOG.info("kafka.key = {}", kafkaKey);

		rotatePartitions = Boolean.parseBoolean(props.getProperty("kafka.rotate", "false").trim());
		
		LOG.info("kafka.rotate = {}", rotatePartitions);

		tcpReceiveBufferBytes = Integer.parseInt(props.getProperty("tcp.receive.buffer.bytes", "" + ONE_MB));
		
		if (tcpReceiveBufferBytes < 1)
		{
			throw new Exception("tcp.receive.buffer.bytes must be positive.  Got " + tcpReceiveBufferBytes);
		}
		
		LOG.info("tcp.receive.buffer.bytes = {}", tcpReceiveBufferBytes);

		maxLineLength = Integer.parseInt(props.getProperty("max.line.length", "" + (64 * 1024)));
		
		if (maxLineLength < 1)
		{
			throw new Exception("max.line.length must be positive.  Got " + maxLineLength);
		}
		
		LOG.info("max.line.length = {}", maxLineLength);

		encodeTimestamp = Boolean.parseBoolean(props.getProperty("encode.timestamp", "true").trim());
		
		LOG.info("encode.timestamp = {}", encodeTimestamp);

		validateUtf8 = Boolean.parseBoolean(props.getProperty("validate.utf8", "true").trim());
		
		LOG.info("validate.utf8 = {}", encodeTimestamp);

		LOG.info("Port to topic mappings:");

		//sources = new ArrayList<? extends Source>();
		
		Set<String> sourceList = props.stringPropertyNames();

		for (String curElement : sourceList)
		{
			Pattern pattern = Pattern.compile("^source\\.([^\\.]+)\\.(port|file)$", Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(curElement);
			
			if (matcher.find())
			{				
				String topic = matcher.group(1);
				String sourceType = matcher.group(2);
				String sourceValue = props.getProperty("source." + topic + "." + sourceType);
				
				Boolean quickRotate = Boolean.parseBoolean(props.getProperty("source." + topic + ".quick.rotate", "false").trim());				
				Long quickRotateMessageBlocks = Long.parseLong(props.getProperty("source." + topic + ".quick.rotate.msgblks", "0").trim());
				
				if (sourceType.equals("port"))
				{					
					PortSource source = new PortSource(sourceValue, topic, quickRotate, quickRotateMessageBlocks);
					portSources.add(source);
					LOG.info("    Adding a new source: {} ", source);
				}
				else if (sourceType.equals("file"))
				{
					File file = new File(sourceValue);
					
					if (file.isDirectory())
					{
						LOG.warn("Skippping file based source for {} because it is a directory", sourceValue);
						continue;
					}
					
					// Get the specified absolute path of the file's parent directory
					
					File parentDirectory = new File(file.getAbsoluteFile().getParentFile().getAbsolutePath());
					
					if (!parentDirectory.exists())
					{
						LOG.warn("Skippping file based source for {} because parent directory {} does not exist", sourceValue, parentDirectory);
						continue;
					}
					
					if (!file.exists())
					{
						LOG.warn("Specified file {} does not exist (although the parrent directory does) will be watched for creation", file);
					}
					
					FileSource source = new FileSource(sourceValue, topic, quickRotate, quickRotateMessageBlocks);
					fileSources.add(source);
					LOG.info("    Adding a new source: {} ", source);						
				}
			}
		}
	}

	public List<PortSource> getPortSources()
	{
		return portSources;
	}

	public void setPortSources(ArrayList<PortSource> sources)
	{
		this.portSources = sources;
	}

	public List<FileSource> getFileSources()
	{
		return fileSources;
	}

	public void setFileSources(ArrayList<FileSource> sources)
	{
		this.fileSources = sources;
	}

	public String getClientId()
	{
		return clientId;
	}

	public void setClientId(String clientId)
	{
		this.clientId = clientId;
	}

	public boolean getKafkaRotatePartitions()
	{
		return rotatePartitions;
	}

	public void setKafkaRotatePartitions(boolean rotatePartitions)
	{
		this.rotatePartitions = rotatePartitions;
	}

	public String getKafkaKey()
	{
		return kafkaKey;
	}

	public void setKafkaKey(String kafkaKey)
	{
		this.kafkaKey = kafkaKey;
	}

	public int getTcpReceiveBufferBytes()
	{
		return tcpReceiveBufferBytes;
	}

	public void setTcpReceiveBufferBytes(int tcpReceiveBufferBytes)
	{
		this.tcpReceiveBufferBytes = tcpReceiveBufferBytes;
	}

	public int getMaxLineLength()
	{
		return maxLineLength;
	}

	public void setMaxLineLength(int maxLineLength)
	{
		this.maxLineLength = maxLineLength;
	}

	public boolean isEncodeTimestamp()
	{
		return encodeTimestamp;
	}

	public void setEncodeTimestamp(boolean encodeTimestamp)
	{
		this.encodeTimestamp = encodeTimestamp;
	}

	public boolean isValidateUtf8()
	{
		return validateUtf8;
	}

	public void setValidateUtf8(boolean validateUtf8)
	{
		this.validateUtf8 = validateUtf8;
	}
}
