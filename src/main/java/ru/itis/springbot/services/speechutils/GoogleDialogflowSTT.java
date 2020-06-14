package ru.itis.springbot.services.speechutils;

import com.google.api.client.util.Maps;
import com.google.api.gax.rpc.BidiStream;
import com.google.cloud.dialogflow.v2.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
public class GoogleDialogflowSTT {
    public static void main(String[] args) throws Exception {
        GoogleDialogflowSTT googleDialogflowSTT = new GoogleDialogflowSTT();
        String projectId = "newagent-oqyfnl";
        String sessionId = "123456789";
        SessionName session = SessionName.of(projectId, sessionId);
        googleDialogflowSTT.detectIntentAudio("recorded2.wav", session);
    }

    @Autowired
    private BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream;

    public static Map<String, QueryResult> detectIntentTexts(
            String projectId,
            List<String> texts,
            String sessionId,
            String languageCode) throws Exception {
        Map<String, QueryResult> queryResults = Maps.newHashMap();
        // Instantiates a client

        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of(projectId, sessionId);
            System.out.println("Session Path: " + session.toString());

            // Detect intents for each text input
            for (String text : texts) {
                // Set the text (hello) and language code (en-US) for the query
                TextInput.Builder textInput =
                        TextInput.newBuilder().setText(text).setLanguageCode(languageCode);


                // Build the query with the TextInput
                QueryInput queryInput = QueryInput.newBuilder().setText(textInput).build();

                // Performs the detect intent request
                DetectIntentResponse response = sessionsClient.detectIntent(session, queryInput);

                // Display the query result
                QueryResult queryResult = response.getQueryResult();

                System.out.println("====================");
                System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                System.out.format("Detected Intent: %s (confidence: %f)\n",
                        queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());

                queryResults.put(text, queryResult);
            }
        }
        return queryResults;
    }

    public QueryResult detectIntentAudio(
            String audioFilePath, SessionName session)
            throws Exception {

        String projectId = "newagent-oqyfnl";
        String sessionId = "123456789";
        String languageCode = "ru-RU";
        // Instantiates a client
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
//            SessionName session = SessionName.of(projectId, sessionId);

            // Note: hard coding audioEncoding and sampleRateHertz for simplicity.
            // Audio encoding of the audio content sent in the query request.
            AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16;
            int sampleRateHertz = 16000;

            // Instructs the speech recognizer how to process the audio content.
            InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                    .setAudioEncoding(audioEncoding) // audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR_16
                    .setLanguageCode(languageCode) // languageCode = "en-US"
                    .setSampleRateHertz(sampleRateHertz) // sampleRateHertz = 16000
                    .build();

            // Build the query with the InputAudioConfig
            QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

            // Read the bytes from the audio file
            byte[] inputAudio = Files.readAllBytes(Paths.get(audioFilePath));

            // Build the DetectIntentRequest
            DetectIntentRequest request = DetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .setInputAudio(ByteString.copyFrom(inputAudio))
                    .build();

            // Performs the detect intent request
            DetectIntentResponse response = sessionsClient.detectIntent(request);

            // Display the query result
            QueryResult queryResult = response.getQueryResult();
            System.out.println("====================");
            System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
            System.out.format("Detected Intent: %s (confidence: %f)\n",
                    queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
            System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());
            return queryResult;
        }
    }

    public void detectIntentStream(String projectId, InputStream audioStream, String sessionId) {
        // String projectId = "YOUR_PROJECT_ID";
        // String audioFilePath = "path_to_your_audio_file";
        // Using the same `sessionId` between requests allows continuation of the conversation.
        // String sessionId = "Identifier of the DetectIntent session";

        // Instantiates a client
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of(projectId, sessionId);

            // Instructs the speech recognizer how to process the audio content.
            // Note: hard coding audioEncoding and sampleRateHertz for simplicity.
            // Audio encoding of the audio content sent in the query request.
            InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR_16)
                    .setLanguageCode("ru-RU") // languageCode = "en-US"
                    .setSampleRateHertz(16000) // sampleRateHertz = 16000
                    .build();

            // Build the query with the InputAudioConfig
            QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

            // Create the Bidirectional stream
//            BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
//                    sessionsClient.streamingDetectIntentCallable().call();

            // The first request must **only** contain the audio configuration:
            bidiStream.send(StreamingDetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .build());

            // Subsequent requests must **only** contain the audio data.
            // Following messages: audio chunks. We just read the file in fixed-size chunks. In reality
            // you would split the user input by time.
            byte[] buffer = new byte[4096];
            int bytes;
            int sendTimes = 0;
            int zeroPackets = 0;

            log.info("waiting for sound to recognize");
            synchronized (audioStream) {
                try {
                    audioStream.wait();
                } catch (InterruptedException e) {
                   throw new IllegalStateException(e);
                }
            }

//                while ((bytes = audioStream.read(buffer)) != -1) {
            while ((bytes = readWithTimeout(audioStream, buffer, 2000)) >= 0) {
                if (isZeroPacket(buffer)) {
                    zeroPackets++;
                }
                if (zeroPackets >= 24) {
                    break;
                }
                log.info("getting bytes: " + buffer[0] + " (already sent " + sendTimes + " times)");
                bidiStream.send(
                        StreamingDetectIntentRequest.newBuilder()
                                .setInputAudio(ByteString.copyFrom(buffer, 0, bytes))
                                .build());
                sendTimes++;
            }


            // Tell the service you are done sending data
            bidiStream.closeSend();
            audioStream.close();
            log.info("stopped sending bytes for recog.");


            for (StreamingDetectIntentResponse response : bidiStream) {
                QueryResult queryResult = response.getQueryResult();
                if(!queryResult.getQueryText().isEmpty()) {
                    System.out.println();
                }
            }
            log.info("reached end of speech recog. response");
            bidiStream.cancel();

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean isZeroPacket(byte[] bytes) {
        for (byte b : bytes) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }

    public static void detectIntentFileStream(String projectId, String audioFilePath, String sessionId) {
        // String projectId = "YOUR_PROJECT_ID";
        // String audioFilePath = "path_to_your_audio_file";
        // Using the same `sessionId` between requests allows continuation of the conversation.
        // String sessionId = "Identifier of the DetectIntent session";

        // Instantiates a client
        try (SessionsClient sessionsClient = SessionsClient.create()) {
            // Set the session name using the sessionId (UUID) and projectID (my-project-id)
            SessionName session = SessionName.of(projectId, sessionId);

            // Instructs the speech recognizer how to process the audio content.
            // Note: hard coding audioEncoding and sampleRateHertz for simplicity.
            // Audio encoding of the audio content sent in the query request.
            InputAudioConfig inputAudioConfig = InputAudioConfig.newBuilder()
                    .setAudioEncoding(AudioEncoding.AUDIO_ENCODING_LINEAR_16)
                    .setLanguageCode("ru-RU") // languageCode = "en-US"
                    .setSampleRateHertz(16000) // sampleRateHertz = 16000
                    .build();

            // Build the query with the InputAudioConfig
            QueryInput queryInput = QueryInput.newBuilder().setAudioConfig(inputAudioConfig).build();

            // Create the Bidirectional stream
            BidiStream<StreamingDetectIntentRequest, StreamingDetectIntentResponse> bidiStream =
                    sessionsClient.streamingDetectIntentCallable().call();

            // The first request must **only** contain the audio configuration:
            bidiStream.send(StreamingDetectIntentRequest.newBuilder()
                    .setSession(session.toString())
                    .setQueryInput(queryInput)
                    .build());

            try (FileInputStream audioStream = new FileInputStream(audioFilePath)) {
                // Subsequent requests must **only** contain the audio data.
                // Following messages: audio chunks. We just read the file in fixed-size chunks. In reality
                // you would split the user input by time.
                byte[] buffer = new byte[4096];
                int bytes;
                while ((bytes = audioStream.read(buffer)) != -1) {
                    bidiStream.send(
                            StreamingDetectIntentRequest.newBuilder()
                                    .setInputAudio(ByteString.copyFrom(buffer, 0, bytes))
                                    .build());
                }
            }

            // Tell the service you are done sending data
            bidiStream.closeSend();

            for (StreamingDetectIntentResponse response : bidiStream) {
                QueryResult queryResult = response.getQueryResult();
                System.out.println("====================");
                System.out.format("Intent Display Name: %s\n", queryResult.getIntent().getDisplayName());
                System.out.format("Query Text: '%s'\n", queryResult.getQueryText());
                System.out.format("Detected Intent: %s (confidence: %f)\n",
                        queryResult.getIntent().getDisplayName(), queryResult.getIntentDetectionConfidence());
                System.out.format("Fulfillment Text: '%s'\n", queryResult.getFulfillmentText());

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int readWithTimeout(InputStream inputStream, byte[] array, int timeout) {
        AtomicReference<Integer> result = new AtomicReference<>();
        synchronized (result) {
            AtomicReference<Thread> t2 = new AtomicReference<>();
            Thread t1 = new Thread(() -> {
                try {
                    synchronized (result) {
                        synchronized (inputStream) {
                            result.set(inputStream.read(array));
                        }
                        t2.get().interrupt();
                        result.notify();
                    }
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
            t2.set(new Thread(() -> {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (result) {
                            result.set(-2);
                            t1.interrupt();
                            result.notify();
                        }
                    }
                }, timeout);
            }));
            t1.start();
            t2.get().start();
            try {
                result.wait();
            } catch (InterruptedException e) {
                throw new IllegalStateException();
            }
        }
        return result.get();
    }

}
