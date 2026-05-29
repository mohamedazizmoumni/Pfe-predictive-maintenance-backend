package com.pfe.predictive.ml.service;

import com.pfe.predictive.data.repository.PredictionRecordRepository;
import com.pfe.predictive.ml.dto.PredictionResponse;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import com.pfe.predictive.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MlPredictionServiceTest {

    @Mock
    private PythonMlClient pythonMlClient;

    @Mock
    private MlMetadataService mlMetadataService;

    @Mock
    private PredictionRecordRepository predictionRecordRepository;

    @Mock
    private NotificationService notificationService;

    private MlPredictionService service;

    @BeforeEach
    void setUp() {
        service = new MlPredictionService(
                pythonMlClient,
                mlMetadataService,
                predictionRecordRepository,
                notificationService
        );
    }

    @Test
    void predictSuccess() {
        when(mlMetadataService.expectedFeatureCount(anyString())).thenReturn(89);
        PredictionResponse expected = new PredictionResponse();
        expected.setPrediction(List.of(12.3, 9.8));
        when(pythonMlClient.predict(any(), anyString(), anyLong())).thenReturn(expected);

        PredictionResponse response = service.predict(1L, List.of(sample(89)), "corr-1", "req-1");

        assertEquals(2, response.getPrediction().size());
        assertEquals(12.3, response.getPrediction().get(0));
    }

    @Test
    void rejectWrongFeatureCount() {
        when(mlMetadataService.expectedFeatureCount(anyString())).thenReturn(89);

        MlBadRequestException ex = assertThrows(MlBadRequestException.class,
            () -> service.predict(1L, List.of(sample(88)), "corr-1", "req-1"));

        assertEquals(true, ex.getMessage().contains("exactly 89 features"));
    }

    @Test
    void rejectEmptyFeatures() {
        when(mlMetadataService.expectedFeatureCount(anyString())).thenReturn(89);

        MlBadRequestException ex = assertThrows(MlBadRequestException.class,
            () -> service.predict(1L, List.of(), "corr-1", "req-1"));

        assertEquals(true, ex.getMessage().contains("at least one sample"));
    }

    @Test
    void rejectInvalidNumericValue() {
        when(mlMetadataService.expectedFeatureCount(anyString())).thenReturn(89);
        List<Double> invalid = sample(89);
        invalid.set(7, Double.NaN);

        MlBadRequestException ex = assertThrows(MlBadRequestException.class,
            () -> service.predict(1L, List.of(invalid), "corr-1", "req-1"));

        assertEquals(true, ex.getMessage().contains("finite numeric value"));
    }

    private List<Double> sample(int size) {
        return java.util.stream.IntStream.range(0, size)
                .mapToDouble(i -> i + 0.1)
                .boxed()
                .collect(java.util.stream.Collectors.toList());
    }
}
