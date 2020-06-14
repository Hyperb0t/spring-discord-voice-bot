package ru.itis.springbot.services;

import com.google.cloud.dialogflow.v2.SessionName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.itis.springbot.dao.RecordRepository;
import ru.itis.springbot.models.Record;
import ru.itis.springbot.services.speechutils.GoogleDialogflowSTT;

import java.util.Optional;

@Service
public class RecordService {

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private GoogleDialogflowSTT googleDialogflowSTT;

    @Autowired
    private SessionName sessionName;

    public void saveRecord(Record record) {
        recordRepository.save(record);
    }

    public String recognizeText(String filename) {
        try {
            return googleDialogflowSTT.detectIntentAudio(filename, sessionName).getQueryText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Record findRecordWithWords(String s) {
        Optional<Record> recordOptional = recordRepository.findByTextContains(s);
        if(recordOptional.isPresent()){
            return recordOptional.get();
        }
        else {
            return null;
        }
    }
}
