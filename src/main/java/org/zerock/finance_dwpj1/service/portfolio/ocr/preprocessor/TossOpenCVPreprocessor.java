package org.zerock.finance_dwpj1.service.portfolio.ocr.preprocessor;

import lombok.extern.slf4j.Slf4j;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;
import org.zerock.finance_dwpj1.service.portfolio.ocr.BrokerType;
import org.zerock.finance_dwpj1.service.portfolio.ocr.OcrPreprocessor;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;
import java.util.List;


@Component
@Slf4j
public class TossOpenCVPreprocessor implements OcrPreprocessor {
    @Override
    public BufferedImage preprocess(BufferedImage input) {

        Mat mat = bufferedImageToMat(input);
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);

        // 2) Gaussian Blur (노이즈 줄이기)
        Imgproc.GaussianBlur(mat, mat, new Size(5, 5), 0);

        // 3) Edge Detection (Canny)
        Mat edges = new Mat();
        Imgproc.Canny(mat, edges, 50, 150);

        // 4) Contours 찾기
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy,
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // 왼쪽 아이콘 영역을 찾기 위한 bounding box 추적
        int leftCropX = 0;

        for (MatOfPoint contour : contours) {
            Rect rect = Imgproc.boundingRect(contour);

            // 왼쪽 영역(0~200px)에 있으면서 가로 70 이상이면 아이콘으로 판단
            if (rect.x < input.getWidth() * 0.25 && rect.width > 70 && rect.height > 70) {
                leftCropX = Math.max(leftCropX, rect.x + rect.width);
            }
        }

        log.info("자동 감지된 left crop 위치 = {}", leftCropX);

        // 아이콘 제거 crop 영역 (왼쪽)
        Rect cropRect = new Rect(leftCropX + 5, 0,
                input.getWidth() - (leftCropX + 5),
                input.getHeight());

        Mat cropped = new Mat(mat, cropRect);

        // 5) Adaptive Threshold (글자만 선명하게 남김)
        Mat th = new Mat();
        Imgproc.adaptiveThreshold(
                cropped, th,
                255,
                Imgproc.ADAPTIVE_THRESH_MEAN_C,
                Imgproc.THRESH_BINARY_INV,
                15, 5
        );

        // 6) 자잘한 노이즈 제거
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(2, 2));
        Imgproc.morphologyEx(th, th, Imgproc.MORPH_OPEN, kernel);

        // Mat → BufferedImage
        return matToBufferedImage(th);
    }

    @Override
    public BrokerType getSupportedBroker() {
        return BrokerType.TOSS;
    }
    private Mat bufferedImageToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }
    private BufferedImage matToBufferedImage(Mat mat) {
        int type = BufferedImage.TYPE_BYTE_GRAY;
        if (mat.channels() > 1) {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }
        BufferedImage image = new BufferedImage(mat.width(), mat.height(), type);
        byte[] data = new byte[mat.width() * mat.height() * (int) mat.elemSize()];
        mat.get(0, 0, data);
        image.getRaster().setDataElements(0, 0, mat.width(), mat.height(), data);
        return image;
    }
}
