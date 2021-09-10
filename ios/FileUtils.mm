//
//  FileUtils.m
//  RNOpencv3
//
//  Created by Adam G Freeman on 11/12/18.
//  Copyright © 2018 Adam G Freeman. All rights reserved.
//

#import "FileUtils.h"
#import "CvCamera.h"
#import "MatManager.h"
#import <Foundation/Foundation.h>

@implementation FileUtils

+ (NSString*)loadBundleResource:(NSString*)filename extension:(NSString*)extension {

    NSBundle *podBundle = [NSBundle bundleForClass:CvCamera.class];
    NSURL *bundleURL = [podBundle URLForResource:@"ocvdata" withExtension:@"bundle"];
    NSBundle *dBundle = [NSBundle bundleWithURL:bundleURL];
    NSString *landmarksPath = [dBundle pathForResource:filename ofType:extension];

    return landmarksPath;
}

// this function makes sure the image is displayed in the correct orientation according
// to its metadata
+ (UIImage*)normalizeImage:(UIImage *)image {
    
    if (image.imageOrientation == UIImageOrientationUp) {
        return image;
    }
    
    UIGraphicsBeginImageContextWithOptions(image.size, false, 0.0/*image.scale*/);
    [image drawInRect:CGRectMake(0.0f, 0.0f, image.size.width, image.size.height)];
    UIImage *normalizedImg = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return normalizedImg;
}

+ (void)imageToMat:(NSString*)inPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject {
    
    // Check input parameters validity
    if (inPath == nil || inPath == (NSString*)NSNull.null || [inPath isEqualToString:@""]) {
        return reject(@"EINVAL", [NSString stringWithFormat:@"EINVAL: invalid parameter, param '%@'", inPath], nil);
    }
    // make sure input exists and is not a directory and output not a dir
    if (![[NSFileManager defaultManager] fileExistsAtPath: inPath]) {
        return reject(@"ENOENT", [NSString stringWithFormat:@"ENOENT: no such file, open '%@'", inPath], nil);
    }
    BOOL isDir = NO;
    if([[NSFileManager defaultManager] fileExistsAtPath:inPath isDirectory:&isDir] && isDir) {
        return reject(@"EISDIR", [NSString stringWithFormat:@"EISDIR: illegal operation on a directory, open '%@'", inPath], nil);
    }
    
    UIImage *sourceImage = [UIImage imageWithContentsOfFile:inPath];
    
    if (sourceImage == nil) {
        return reject(@"ENOENT", [NSString stringWithFormat:@"ENOENT: no such file, open '%@'", inPath], nil);
    }
    
    UIImage *normalizedImage = [FileUtils normalizeImage:sourceImage];
    
    Mat outputMat;
    UIImageToMat(normalizedImage, outputMat);
    int matIndex = [MatManager.sharedMgr addMat:outputMat];
    
    NSNumber *wid = [NSNumber numberWithInt:(int)sourceImage.size.width];
    NSNumber *hei = [NSNumber numberWithInt:(int)sourceImage.size.height];
    NSNumber *matI = [NSNumber numberWithInt:matIndex];
    
    NSDictionary *returnDict = @{ @"cols" : wid, @"rows" : hei, @"matIndex" : matI };
    resolve(returnDict);
}

+ (void)matToImage:(MatWrapper*)inputMatWrapper outPath:(NSString*)outPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject {
    
    Mat inputMat = inputMatWrapper.myMat;
    
    UIImage *destImage = MatToUIImage(inputMat);
    if (destImage == nil) {
        return reject(@"ENOENT", [NSString stringWithFormat:@"ENOENT: no such file, open '%@'", destImage], nil);
    }
    
    NSString *fileType = [[outPath lowercaseString] pathExtension];
    if ([fileType isEqualToString:@"png"]) {
        [UIImagePNGRepresentation(destImage) writeToFile:outPath atomically:YES];
    }
    else if ([fileType isEqualToString:@"jpg"] || [fileType isEqualToString:@"jpeg"]) {
        [UIImageJPEGRepresentation(destImage, 80) writeToFile:outPath atomically:YES];
        //UIImageWriteToSavedPhotosAlbum(destImage, self, nil, nil);
    }
    else {
        return reject(@"EINVAL", [NSString stringWithFormat:@"EINVAL: unsupported file type, write '%@'", fileType], nil);
    }
    
    NSNumber *wid = [NSNumber numberWithInt:(int)destImage.size.width];
    NSNumber *hei = [NSNumber numberWithInt:(int)destImage.size.height];
    
    NSDictionary *returnDict = @{ @"width" : wid, @"height" : hei,
                                  @"uri" : outPath };
    
    resolve(returnDict);
}

+ (void)demoOpencvMethod:(MatWrapper*)inputMatWrapper outPath:(NSString*)outPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject {
    Mat inputMat = inputMatWrapper.myMat;
    Mat backup = inputMatWrapper.myMat;
    
    cv::Mat imgOriginal;        // input image
    cv::Mat imgGrayscale;        // grayscale of input image
    cv::Mat imgBlurred;            // intermediate blured image
    cv::Mat imgCanny;            // Canny edge image
    cv::Mat bitwiseOrMat;

    cv::Point p1(0,0), p2(600,600);
    cv::Scalar colorLine(0,255,0);

    cv::cvtColor(inputMat, imgGrayscale, CV_BGR2GRAY);

    cv::GaussianBlur(imgGrayscale,            // input image
        imgBlurred,                            // output image
        cv::Size(5, 5),                        // smoothing window width and height in pixels
        1.5);                                // sigma value, determines how much the image will be blurred

    cv::Canny(imgBlurred,            // input image
        imgCanny,                    // output image
        50,                        // low threshold
        120);                        // high threshold

    std::vector<std::vector<cv::Point>> contours;
    std::vector<Vec4i> hierarchy;
    findContours(imgCanny, contours, hierarchy, RETR_TREE, CHAIN_APPROX_NONE);
    drawContours(backup, contours, -1, Scalar(0, 255, 0), 3);


    UIImage *destImage = MatToUIImage(backup);
    if (destImage == nil) {
        return reject(@"ENOENT", [NSString stringWithFormat:@"ENOENT: no such file, open '%@'", destImage], nil);
    }
    
    NSString *fileType = [[outPath lowercaseString] pathExtension];
    if ([fileType isEqualToString:@"png"]) {
        [UIImagePNGRepresentation(destImage) writeToFile:outPath atomically:YES];
    }
    else if ([fileType isEqualToString:@"jpg"] || [fileType isEqualToString:@"jpeg"]) {
        [UIImageJPEGRepresentation(destImage, 80) writeToFile:outPath atomically:YES];
        //UIImageWriteToSavedPhotosAlbum(destImage, self, nil, nil);
    }
    else {
        return reject(@"EINVAL", [NSString stringWithFormat:@"EINVAL: unsupported file type, write '%@'", fileType], nil);
    }
    
    NSNumber *wid = [NSNumber numberWithInt:(int)destImage.size.width];
    NSNumber *hei = [NSNumber numberWithInt:(int)destImage.size.height];
    
    NSDictionary *returnDict = @{ @"width" : wid, @"height" : hei,
                                  @"uri" : outPath };
    
    resolve(returnDict);
}

@end

