//
//  CustomCameraViewController.h
//  CustomCamera
//
//  Created by Chris van Es on 24/02/2014.
//
//

#import <UIKit/UIKit.h>

@interface MBPCameraViewController : UIViewController

- (id)initWithCallback:(void(^)(UIImage*))callback titleName:(NSString*)title_ logoFilename:(NSString*)logoFilename_;

@end
