package com.gittowork.global.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.gittowork.global.exception.S3DeleteException;
import com.gittowork.global.exception.S3UploadException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * AWS S3에 파일을 업로드 및 삭제하는 기능을 제공하는 서비스 클래스입니다.
 */
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucketName}")
    private String bucketName;

    /**
     * MultipartFile을 S3에 업로드하고 파일 URL을 반환합니다.
     *
     * @param file 업로드할 파일 객체
     * @return S3에 저장된 파일의 URL
     * @throws S3UploadException 파일 업로드 중 오류가 발생한 경우
     */
    public String uploadFileToS3(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String fileExtension = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : "";
        String s3FileName = UUID.randomUUID().toString().substring(0, 10) + fileExtension;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());

        try {
            PutObjectRequest request = new PutObjectRequest(bucketName, s3FileName, file.getInputStream(), metadata);
            amazonS3.putObject(request);
        } catch (IOException e) {
            throw new S3UploadException("S3 업로드에 실패했습니다.");
        }
        return amazonS3.getUrl(bucketName, s3FileName).toString();
    }

    /**
     * S3에 저장된 파일을 삭제합니다.
     *
     * @param fileUrl 삭제할 파일의 URL
     * @throws S3DeleteException 파일 삭제 중 오류가 발생한 경우
     */
    public void deleteCoverLetterFromS3(String fileUrl) {
        String key = getKeyFromCoverLetterAddress(fileUrl);
        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucketName, key));
        } catch (Exception e) {
            throw new S3DeleteException("S3 삭제에 실패했습니다.");
        }
    }

    /**
     * 파일 URL로부터 S3 객체 키(key)를 추출합니다.
     *
     * @param fileUrl S3 파일의 URL
     * @return S3 객체 키 문자열
     * @throws S3DeleteException URL이 올바르지 않아 키를 파싱하지 못한 경우
     */
    private String getKeyFromCoverLetterAddress(String fileUrl) {
        try {
            URL url = new URL(fileUrl);
            String decodedPath = URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8);
            return decodedPath.startsWith("/") ? decodedPath.substring(1) : decodedPath;
        } catch (MalformedURLException e) {
            throw new S3DeleteException("S3 키 추출에 실패했습니다.");
        }
    }
}
