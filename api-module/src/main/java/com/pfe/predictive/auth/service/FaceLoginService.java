package com.pfe.predictive.auth.service;

import com.pfe.predictive.auth.client.FaceRecognitionClient;
import com.pfe.predictive.auth.dto.FaceLoginResponse;
import com.pfe.predictive.auth.dto.FaceRecognitionResult;
import com.pfe.predictive.auth.dto.LoginResponse;
import com.pfe.predictive.auth.exception.FaceLoginUnauthorizedException;
import com.pfe.predictive.core.entity.User;
import com.pfe.predictive.data.repository.UserRepository;
import com.pfe.predictive.ml.exception.MlBadRequestException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FaceLoginService {

    private final FaceRecognitionClient faceRecognitionClient;
    private final UserRepository userRepository;
    private final AuthService authService;

    public FaceLoginService(FaceRecognitionClient faceRecognitionClient,
                            UserRepository userRepository,
                            AuthService authService) {
        this.faceRecognitionClient = faceRecognitionClient;
        this.userRepository = userRepository;
        this.authService = authService;
    }

    public FaceLoginResponse loginWithFace(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new MlBadRequestException("Image file is required");
        }

        FaceRecognitionResult recognitionResult = faceRecognitionClient.recognize(image);
        Long recognizedUserId = recognitionResult.getUserId();

        if (recognizedUserId == null || recognizedUserId <= 0) {
            throw new FaceLoginUnauthorizedException("No matching face/user was found");
        }

        User user = userRepository.findById(recognizedUserId)
                .orElseThrow(() -> new FaceLoginUnauthorizedException("No matching face/user was found"));

        LoginResponse loginResponse = authService.createLoginResponseForUser(user);
        return FaceLoginResponse.from(loginResponse, recognitionResult.getConfidence());
    }
}
