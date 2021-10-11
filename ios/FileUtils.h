//
//  FileUtils.h
//  RNOpencv3
//
//  Created by Adam G Freeman on 11/12/18.
//  Copyright © 2018 Adam G Freeman. All rights reserved.
//

#ifndef FileUtils_h
#define FileUtils_h

//#import <React/RCTEventEmitter.h>
#import <React/RCTBridge.h>
#import <UIKit/UIKit.h>

@class MatWrapper;

@interface FileUtils : NSObject

+ (NSString*)loadBundleResource:(NSString*)filename extension:(NSString*)extension;

+ (UIImage*)normalizeImage:(UIImage*)image;

+ (void)matToImage:(MatWrapper*)inputMat outPath:(NSString*)outPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

+ (void)imageToMat:(NSString*)inPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

+ (void)demoOpencvMethod:(MatWrapper*)inputMatWrapper outPath:(NSString*)outPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

+ (void)ROGaussianBlur:(MatWrapper*)inputMat outPath:(NSString*)outPath gaussian:(int)gaussian resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

+ (void)ROCanny:(MatWrapper*)originalImage bluredImage:(MatWrapper*)bluredImage outPath:(NSString*)outPath cannyPath:(NSString*)cannyPath min:(int)min max:(int)max resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

+ (void)ROCrop:(NSString*)imagePath outPath:(NSString*)outPath x:(int)x y:(int)y width:(int)width height:(int)height resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

+ (void)ROCombain:(NSString*)firstImage secondImage:(NSString*)secondImage outPath:(NSString*)outPath resolver:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject;

@end
#endif /* FileUtils_h */
