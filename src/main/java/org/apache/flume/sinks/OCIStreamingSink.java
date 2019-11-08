package org.apache.flume.sinks;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.Configurable;
import org.apache.flume.sink.AbstractSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import com.oracle.bmc.streaming.StreamAdminClient;
import com.oracle.bmc.streaming.StreamClient;
import com.oracle.bmc.streaming.model.PutMessagesDetailsEntry;
import com.oracle.bmc.streaming.model.Stream;
import com.oracle.bmc.streaming.model.Stream.LifecycleState;
import com.oracle.bmc.streaming.requests.GetStreamRequest;
import com.oracle.bmc.streaming.requests.ListStreamsRequest;
import com.oracle.bmc.streaming.responses.GetStreamResponse;
import com.oracle.bmc.streaming.responses.ListStreamsResponse;

public class OCIStreamingSink extends AbstractSink implements Configurable {

	private final static Logger logger = LoggerFactory.getLogger(OCIStreamingSink.class);

	private String compartmentId;
	private String streamName;
	private String key;
	private AuthenticationDetailsProvider provider;
	private StreamAdminClient adminClient;

	final String configurationFilePath = "~/.oci/config";
	final String profile = "DEFAULT";

	public void configure(Context context) {
		compartmentId = context.getString("compartmentId");
		streamName = context.getString("streamName");

		try {
			provider = new ConfigFileAuthenticationDetailsProvider(configurationFilePath, profile);
			adminClient = new StreamAdminClient(provider);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@Override
	public synchronized void start() {
		super.start();
	}

	@Override
	public synchronized void stop() {
		super.stop();
	}

	public Status process() throws EventDeliveryException {
		Status status = null;

		// Start transaction
		Channel ch = getChannel();
		Transaction txn = ch.getTransaction();
		txn.begin();
		try {

			ListStreamsRequest listRequest =
					ListStreamsRequest.builder()
					.compartmentId(compartmentId)
					.lifecycleState(LifecycleState.Active)
					.name(streamName)
					.build();

			ListStreamsResponse listResponse = adminClient.listStreams(listRequest);
			if (!listResponse.getItems().isEmpty()) {

				String streamId = listResponse.getItems().get(0).getId();
				Stream stream = getStream(adminClient, streamId);

				StreamClient streamClient = new StreamClient(provider);
				streamClient.setEndpoint(stream.getMessagesEndpoint());

				List<PutMessagesDetailsEntry> messages = new ArrayList<PutMessagesDetailsEntry>();

				Event event = ch.take();
				if (event == null) {
					String value = new String(event.getBody());

					messages.add(PutMessagesDetailsEntry.builder()
							.key(String.format("messageKey-%s", key).getBytes(UTF_8))
							.value(String.format("messageValue-%s", value).getBytes(UTF_8))
							.build());

				}

			}


			txn.commit();
			status = Status.READY;
		} catch (Throwable t) {
			txn.rollback();

			// Log exception, handle individual exceptions as needed
			logger.warn("Unknown exception, txn rolled-back", t);

			status = Status.BACKOFF;

			// re-throw all Errors
			if (t instanceof Error) {
				throw (Error) t;
			}
		} finally {
			txn.close();
		}
		return status;
	}

	private static Stream getStream(StreamAdminClient adminClient, String streamId) {
		GetStreamResponse getResponse = adminClient.getStream(GetStreamRequest.builder().streamId(streamId).build());
		return getResponse.getStream();
	}

}