package org.catalysts.commengage.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.catalysts.commengage.config.AppConfig;
import org.catalysts.commengage.contract.qrd.*;
import org.catalysts.commengage.domain.CodedLocation;
import org.catalysts.commengage.domain.GoogleReverseGeoResponse;
import org.catalysts.commengage.domain.QRCode;
import org.catalysts.commengage.repository.*;
import org.catalysts.commengage.util.FileUtil;
import org.catalysts.commengage.util.ObjectMapperFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@SpringBootTest
class EndToEndTest {
    @Autowired
    private QRCodeRepository qrCodeRepository;
    @Autowired
    private UserRequestRepository userRequestRepository;
    @Autowired
    private CodedLocationRepository codedLocationRepository;
    @Autowired
    private AppConfig appConfig;

    @Test
    public void processQrCodes() {
        QrdProcessor qrdProcessor = new QrdProcessor(QrdApiRepositoryStub.withNewRequests(), qrCodeRepository, userRequestRepository, codedLocationRepository);
        CodedLocationProcessor codedLocationProcessor = new CodedLocationProcessor(codedLocationRepository, new GoogleReverseGeoRepositoryStub(appConfig), appConfig);

        qrdProcessor.processQrCodes();
        List<QRCode> qrCodes = qrCodeRepository.findAllBy();
        assertEquals(2, qrCodes.size());
        assertEquals(10, qrCodes.get(0).getRequestsOffset());
        assertEquals(10, qrCodes.get(1).getRequestsOffset());
        assertEquals(20, userRequestRepository.findAllBy().size());

        assertNotEquals(0, codedLocationRepository.findAllByNumberOfTimesLookedUpEquals(0).size());
        List<CodedLocation> nearExpiringAndNewLocations = codedLocationRepository.getNearExpiringAndNewLocations(2);
        assertEquals(7, nearExpiringAndNewLocations.size());
        codedLocationProcessor.process();
        assertEquals(0, codedLocationRepository.findAllByNumberOfTimesLookedUpEquals(0).size());
        assertEquals(7, codedLocationRepository.findAllBy().size());
        assertEquals(0, codedLocationRepository.getNearExpiringAndNewLocations(2).size());

//        Second Run
        qrdProcessor = new QrdProcessor(QrdApiRepositoryStub.withIncrementalUserRequests(), qrCodeRepository, userRequestRepository, codedLocationRepository);
        qrdProcessor.processQrCodes();
        qrCodes = qrCodeRepository.findAllBy();
        assertEquals(2, qrCodes.size());
        assertEquals(10, qrCodes.get(0).getRequestsOffset());
        assertEquals(11, qrCodes.get(1).getRequestsOffset());
        assertEquals(21, userRequestRepository.findAllBy().size());

        assertEquals(1, codedLocationRepository.findAllByNumberOfTimesLookedUpEquals(0).size());
        assertEquals(8, codedLocationRepository.findAllBy().size());
        assertEquals(1, codedLocationRepository.getNearExpiringAndNewLocations(2).size());
        codedLocationProcessor.process();
        assertEquals(0, codedLocationRepository.findAllByNumberOfTimesLookedUpEquals(0).size());
        assertEquals(8, codedLocationRepository.findAllBy().size());
        assertEquals(0, codedLocationRepository.getNearExpiringAndNewLocations(2).size());
    }

    public static class GoogleReverseGeoRepositoryStub extends GoogleReverseGeoRepository {
        public GoogleReverseGeoRepositoryStub(AppConfig appConfig) {
            super(null, appConfig);
        }

        @Override
        public GoogleReverseGeoResponse getReverseGeocode(CodedLocation codedLocation) {
            try {
                String s = FileUtil.readFile("/stubbedGoogleResponse.json");
                return ObjectMapperFactory.OBJECT_MAPPER.readValue(s, GoogleReverseGeoResponse.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class QrdApiRepositoryStub extends QrdApiRepository {
        private final HashMap<String, List<UserRequestResponse>> userRequestsMap = new HashMap<>();
        private List<QRCodeResponse> qrCodeResponses;

        public static QrdApiRepositoryStub withNewRequests() {
            QrdApiRepositoryStub qrdApiRepositoryStub = new QrdApiRepositoryStub();
            qrdApiRepositoryStub.userRequestsMap.put("iec1", getUserRequests("/userRequests-iec1-1.json"));
            qrdApiRepositoryStub.userRequestsMap.put("nregaiec", getUserRequests("/userRequests-nregaiec-1.json"));
            qrdApiRepositoryStub.qrCodeResponses = createQrCodesResponse("/qrCodes-1.json");
            return qrdApiRepositoryStub;
        }

        public static QrdApiRepositoryStub withIncrementalUserRequests() {
            QrdApiRepositoryStub qrdApiRepositoryStub = new QrdApiRepositoryStub();
            qrdApiRepositoryStub.userRequestsMap.put("iec1", getUserRequests("/userRequests-iec1-2.json"));
            qrdApiRepositoryStub.userRequestsMap.put("nregaiec", getUserRequests("/userRequests-nregaiec-2.json"));
            qrdApiRepositoryStub.qrCodeResponses = createQrCodesResponse("/qrCodes-2.json");
            return qrdApiRepositoryStub;
        }

        private static List<UserRequestResponse> getUserRequests(String fileName) {
            try {
                String s = FileUtil.readFile(fileName);
                QRDContainer<QRCodeDetailsDto> response = ObjectMapperFactory.OBJECT_MAPPER.readValue(s, new TypeReference<>() {
                });
                return response.getResult().getRequests();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<QRCodeResponse> getQRCodes() {
            return qrCodeResponses;
        }

        private static List<QRCodeResponse> createQrCodesResponse(String fileName) {
            try {
                String s = FileUtil.readFile(fileName);
                QRDContainer<QRCodesListingDto> response = ObjectMapperFactory.OBJECT_MAPPER.readValue(s, new TypeReference<>() {
                });
                return response.getResult().getQrcodes();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public List<UserRequestResponse> getQRCodeDetails(String qrCodeId, int limit, int requestsOffset) {
            return userRequestsMap.get(qrCodeId);
        }
    }
}
