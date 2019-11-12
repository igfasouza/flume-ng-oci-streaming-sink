package org.apache.flume.source;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.PollableSource;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.SimpleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.streaming.StreamAdminClient;
import com.oracle.bmc.streaming.StreamClient;
import com.oracle.bmc.streaming.model.CreateCursorDetails;
import com.oracle.bmc.streaming.model.CreateCursorDetails.Type;
import com.oracle.bmc.streaming.model.Message;
import com.oracle.bmc.streaming.model.Stream;
import com.oracle.bmc.streaming.model.Stream.LifecycleState;
import com.oracle.bmc.streaming.requests.CreateCursorRequest;
import com.oracle.bmc.streaming.requests.GetMessagesRequest;
import com.oracle.bmc.streaming.requests.GetStreamRequest;
import com.oracle.bmc.streaming.requests.ListStreamsRequest;
import com.oracle.bmc.streaming.responses.CreateCursorResponse;
import com.oracle.bmc.streaming.responses.GetMessagesResponse;
import com.oracle.bmc.streaming.responses.GetStreamResponse;
import com.oracle.bmc.streaming.responses.ListStreamsResponse;

/**
 *
 * @author jjmartinez jjmartinez@keedio.com - KEEDIO
 *
 */
public class OCIStreamingSource extends AbstractSource implements Configurable, PollableSource {

	private final static Logger logger = LoggerFactory.getLogger(OCIStreamingSource.class);

	private String compartmentId;
	private String streamName;
	private AuthenticationDetailsProvider provider;
	private StreamAdminClient adminClient;
	private ListStreamsResponse listResponse;
	private String partition;

	final String configurationFilePath = "~/.oci/config";
	final String profile = "DEFAULT";

	@Override
	public void configure(Context context) {
		logger.info("configure ");

		compartmentId = context.getString("compartmentId");
		streamName = context.getString("streamName");
		partition = context.getString("partition");

		try {
			provider = new ConfigFileAuthenticationDetailsProvider(configurationFilePath, profile);
			adminClient = new StreamAdminClient(provider);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		ListStreamsRequest listRequest =
				ListStreamsRequest.builder()
				.compartmentId(compartmentId)
				.lifecycleState(LifecycleState.Active)
				.name(streamName)
				.build();

		listResponse = adminClient.listStreams(listRequest);

	}

	@Override
	public void start() {
		logger.info("Start custom flume source");
		super.start();
	}

	@Override
	public void stop () {
		logger.info("stop ");
		super.stop();
	}

	@Override
	public Status process() throws EventDeliveryException {
		logger.info("process ");

		Status status = null;
		List<Event> eventList = new ArrayList<Event>();

		try {
			Event event = new SimpleEvent();

			if (!listResponse.getItems().isEmpty()) {

				String streamId = listResponse.getItems().get(0).getId();
				Stream stream = getStream(adminClient, streamId);

				StreamClient streamClient = new StreamClient(provider);
				streamClient.setEndpoint(stream.getMessagesEndpoint());

				CreateCursorDetails cursorDetails =
						CreateCursorDetails.builder().partition(partition).type(Type.TrimHorizon).build();

				CreateCursorRequest createCursorRequest =
						CreateCursorRequest.builder()
						.streamId(streamId)
						.createCursorDetails(cursorDetails)
						.build();

				CreateCursorResponse cursorResponse = streamClient.createCursor(createCursorRequest);

				String partitionCursor = cursorResponse.getCursor().getValue();
				String cursor = partitionCursor;

				GetMessagesRequest getRequest =
						GetMessagesRequest.builder()
						.streamId(streamId)
						.cursor(cursor)
						.limit(10)
						.build();

				GetMessagesResponse getResponse = streamClient.getMessages(getRequest);

				for (Message message : getResponse.getItems()) {
					if(message.getValue() != null) {
						logger.info("message " + new String(message.getValue(), UTF_8));
						event.setBody(message.getValue());
						Map<String, String> headers = new HashMap<String, String>();
						headers.put("timestamp", String.valueOf(System.currentTimeMillis()));
						event.setHeaders(headers);
						eventList.add(event);
						getChannelProcessor().processEventBatch(eventList);
					}else {
						logger.info("message " + message);
					}
					
				}

			}

			status = Status.READY;
		} catch (Throwable t) {
			logger.error("ERROR: ",t);
			status = Status.BACKOFF;
		}
		return status;
	}

	private static Stream getStream(StreamAdminClient adminClient, String streamId) {
		GetStreamResponse getResponse = adminClient.getStream(GetStreamRequest.builder().streamId(streamId).build());
		return getResponse.getStream();
	}

}
