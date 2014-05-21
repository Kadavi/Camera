//
//  CustomCamera.m
//  CustomCamera
//
//  Created by Chris van Es on 24/02/2014.
//
//

#import "MBPCamera.h"
#import "MBPCameraViewController.h"

@implementation MBPCamera

- (void)takePicture:(CDVInvokedUrlCommand*)command {
    NSString *title = [command argumentAtIndex:0];
    CGFloat quality = [[command argumentAtIndex:1] floatValue];
    CGFloat targetWidth = [[command argumentAtIndex:2] floatValue];
    CGFloat targetHeight = [[command argumentAtIndex:3] floatValue];
    NSString *logoFilename = [command argumentAtIndex: 4];
    //NSString *description  = [command argumentAtIndex: 5];
    
    if (![UIImagePickerController isCameraDeviceAvailable:UIImagePickerControllerCameraDeviceRear]) {
        // no rear camera detected
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"No rear camera detected"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else if (![UIImagePickerController isSourceTypeAvailable:UIImagePickerControllerSourceTypeCamera]) {
        // camera is not accessible
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"Camera is not accessible"];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
    } else {
        MBPCameraViewController *cameraViewController = [[MBPCameraViewController alloc] initWithCallback:^(UIImage *image) {
            UIImage *scaledImage = [self scaleImage:image toSize:CGSizeMake(targetWidth, targetHeight)];
            NSData *scaledImageData = UIImageJPEGRepresentation(scaledImage, quality / 100);
            // convert NSData to base64 string
            NSString *base64scaledImageData;
            if ([scaledImageData respondsToSelector:@selector(base64EncodedStringWithOptions:)]) {
                base64scaledImageData = [scaledImageData base64EncodedStringWithOptions:kNilOptions]; //ios 7+
            } else {
                base64scaledImageData = [scaledImageData base64Encoding]; // pre ios 7
            }
            
            CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK
                                                        messageAsString: base64scaledImageData];
            [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            [self.viewController dismissViewControllerAnimated:YES completion:nil];
        } titleName:title logoFilename:logoFilename ];
        [self.viewController presentViewController:cameraViewController animated:YES completion:nil];
    }
}


- (UIImage*)scaleImage:(UIImage*)image toSize:(CGSize)targetSize {
    if (targetSize.width <= 0 && targetSize.height <= 0) {
        return image;
    }
    
    CGFloat aspectRatio = image.size.height / image.size.width;
    CGSize scaledSize;
    if (targetSize.width > 0 && targetSize.height <= 0) {
        scaledSize = CGSizeMake(targetSize.width, targetSize.width * aspectRatio);
    } else if (targetSize.width <= 0 && targetSize.height > 0) {
        scaledSize = CGSizeMake(targetSize.height / aspectRatio, targetSize.height);
    } else {
        scaledSize = CGSizeMake(targetSize.width, targetSize.height);
    }
    
    UIGraphicsBeginImageContext(scaledSize);
    [image drawInRect:CGRectMake(0, 0, scaledSize.width, scaledSize.height)];
    UIImage *scaledImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return scaledImage;
}

@end