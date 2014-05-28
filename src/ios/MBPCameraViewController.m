#import "MBPCameraViewController.h"

#import <Cordova/CDV.h>
#import <AVFoundation/AVFoundation.h>

@implementation MBPCameraViewController {
    void(^_callback)(UIImage*);
    AVCaptureSession *_captureSession;
    AVCaptureDevice *_rearCamera;
    AVCaptureStillImageOutput *_stillImageOutput;
    AVCaptureVideoPreviewLayer *previewLayer;
    UIView *_buttonPanel;
    UIView *_headerPanel;
    UIImageView *_logoPanel;
    UILabel *_titlePanel;
    UIView *_topFramePanel;
    UIView *_leftFramePanel;
    UIButton *_captureButton;
    UIButton *_cancelButton;
    NSString *_logoFilename;
    NSString *_title;
    UIButton *_backButton;
    UIColor *_headerPanelColor;
    UIColor *_framePanelColor;
    UIActivityIndicatorView *_activityIndicator;
}
static const CGFloat kHeaderHeightPhone = 56;
static const CGFloat kFrameBorderSizePhone = 20;
static const CGFloat kTitleFontSize = 14;
static const CGFloat kCaptureButtonWidthPhone = 120;
static const CGFloat kCaptureButtonHeightPhone = 36;
static const CGFloat kCaptureButtonRadiusPhone = 12;

//static const CGFloat kBackButtonWidthPhone = 100;
//static const CGFloat kBackButtonHeightPhone = 40;
static const CGFloat kBorderImageWidthPhone = 50;
static const CGFloat kBorderImageHeightPhone = 50;
static const CGFloat kHorizontalInsetPhone = 15;
static const CGFloat kVerticalInsetPhone = 25;
static const CGFloat kCaptureButtonVerticalInsetPhone = 6;

static const CGFloat kCaptureButtonWidthTablet = 75;
static const CGFloat kCaptureButtonHeightTablet = 75;
//static const CGFloat kBackButtonWidthTablet = 150;
//static const CGFloat kBackButtonHeightTablet = 50;
static const CGFloat kBorderImageWidthTablet = 50;
static const CGFloat kBorderImageHeightTablet = 50;
static const CGFloat kHorizontalInsetTablet = 100;
static const CGFloat kVerticalInsetTablet = 50;
static const CGFloat kCaptureButtonVerticalInsetTablet = 20;

static const CGFloat kAspectRatio = 125.0f / 86;

- (id)initWithCallback:(void(^)(UIImage*))callback titleName:(NSString*)title_ logoFilename:(NSString*)logoFilename_ {
    self = [super initWithNibName:nil bundle:nil];
    if (self) {
        _callback = callback;
        _captureSession = [[AVCaptureSession alloc] init];
        _captureSession.sessionPreset = AVCaptureSessionPresetPhoto;
        _title = title_;
        _logoFilename = logoFilename_;
        _headerPanelColor = [UIColor colorWithRed: 45.0/255.0 green: 68.0/255.0 blue:82.0/255.0 alpha:1.0f];
        _framePanelColor = [UIColor colorWithRed: 185.0/255.0 green: 199.0/255.0 blue:212.0/255.0 alpha:150.0/255.0];
    }
    return self;
}

- (void)dealloc {
    [_captureSession stopRunning];
}

- (void)loadView {
    self.view = [[UIView alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    self.view.backgroundColor = [UIColor blackColor];
    previewLayer = [AVCaptureVideoPreviewLayer layerWithSession:_captureSession];
    previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill;
    previewLayer.frame = self.view.bounds;
    [[self.view layer] addSublayer:previewLayer];
    [self.view addSubview:[self createOverlay]];
    _activityIndicator = [[UIActivityIndicatorView alloc] initWithActivityIndicatorStyle:UIActivityIndicatorViewStyleWhiteLarge];
    _activityIndicator.center = self.view.center;
    [self.view addSubview:_activityIndicator];
    [_activityIndicator startAnimating];
}

- (UIView*)createOverlay {
    UIView *overlay = [[UIView alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
    
    _headerPanel = [[UIImageView alloc] initWithFrame:CGRectZero];
    [_headerPanel setBackgroundColor: _headerPanelColor];
    
    UIImage *logoImage = [UIImage imageNamed: _logoFilename];
    UIImage *logoImageScaled = [UIImage imageWithCGImage:[logoImage CGImage] scale:(logoImage.scale * 1.5) orientation:(logoImage.imageOrientation)];
    _logoPanel = [[UIImageView alloc] initWithFrame:CGRectZero];
    [_logoPanel setImage: logoImageScaled];
    [_logoPanel setTransform:CGAffineTransformMakeRotation(90 * M_PI / 180)];
    [_headerPanel addSubview:_logoPanel];
    [overlay addSubview:_headerPanel];
    
    _topFramePanel = [[UIView alloc] initWithFrame:CGRectZero];
    [_topFramePanel setBackgroundColor: _framePanelColor];
    [overlay addSubview:_topFramePanel];
    
    _leftFramePanel = [[UIView alloc] initWithFrame:CGRectZero];
    [_leftFramePanel setBackgroundColor: _framePanelColor];
    [overlay addSubview:_leftFramePanel];
    
    _titlePanel = [[UILabel alloc] initWithFrame:CGRectZero];
    [_titlePanel setText:_title];
    [_titlePanel setFont:[UIFont boldSystemFontOfSize: kTitleFontSize]];
    [_titlePanel setTextColor: [UIColor colorWithWhite: 1.0f alpha: 1.0f ]];
    [_titlePanel setTransform: CGAffineTransformMakeRotation(90 * M_PI / 180)];
    [_titlePanel setBackgroundColor: _framePanelColor];
    [overlay addSubview:_titlePanel];
    
    _buttonPanel = [[UIView alloc] initWithFrame:CGRectZero];
    [_buttonPanel setBackgroundColor:_framePanelColor];
    [overlay addSubview:_buttonPanel];
    
    _captureButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [_captureButton setTitle:@"Take Picture" forState:UIControlStateNormal];

    [_captureButton addTarget:self action:@selector(takePictureWaitingForCameraToFocus) forControlEvents:UIControlEventTouchUpInside];
    
    [overlay addSubview:_captureButton];
    
    _cancelButton = [UIButton buttonWithType:UIButtonTypeCustom];
    [_cancelButton setTitle:@"Cancel" forState:UIControlStateNormal];
    [_cancelButton setBackgroundColor:[UIColor colorWithWhite:0.0f alpha:0.0f]];
    [_cancelButton addTarget:self action:@selector(cancelTakePicture) forControlEvents:UIControlEventTouchUpInside];
    
    [overlay addSubview:_cancelButton];
    
    // <-- Draw crop marks -->
    CGRect bounds = [[UIScreen mainScreen] bounds];
    NSInteger height = bounds.size.height;
    NSInteger width = bounds.size.width - kHeaderHeightPhone;
    
	CGPoint topLeftPts = CGPointMake(kFrameBorderSizePhone, kFrameBorderSizePhone);
	CGPoint topRightPts = CGPointMake(width - kFrameBorderSizePhone, kFrameBorderSizePhone);
	CGPoint bottomLeftPts = CGPointMake(kFrameBorderSizePhone, height - kCaptureButtonHeightPhone - (kCaptureButtonVerticalInsetPhone * 2));
	CGPoint bottomRightPts = CGPointMake(width - kFrameBorderSizePhone, height - kCaptureButtonHeightPhone - (kCaptureButtonVerticalInsetPhone * 2));
    
    CGMutablePathRef cropMarkPath = CGPathCreateMutable();
    
    CGFloat borderLineLength = 30;
    
    // Top left corner marks
	CGPathMoveToPoint(cropMarkPath, NULL, topLeftPts.x, topLeftPts.y + borderLineLength);
	CGPathAddLineToPoint(cropMarkPath, NULL, topLeftPts.x, topLeftPts.y);
	CGPathAddLineToPoint(cropMarkPath, NULL, topLeftPts.x + borderLineLength, topLeftPts.y);
    
    // Top right corner marks
    CGPathMoveToPoint(cropMarkPath, NULL, topRightPts.x - borderLineLength, topRightPts.y);
	CGPathAddLineToPoint(cropMarkPath, NULL, topRightPts.x, topRightPts.y);
	CGPathAddLineToPoint(cropMarkPath, NULL, topRightPts.x, topRightPts.y + borderLineLength);
    
    // Bottom right mark
	CGPathMoveToPoint(cropMarkPath, NULL, bottomRightPts.x, bottomRightPts.y - borderLineLength);
	CGPathAddLineToPoint(cropMarkPath, NULL, bottomRightPts.x, bottomRightPts.y);
	CGPathAddLineToPoint(cropMarkPath, NULL, bottomRightPts.x - borderLineLength, bottomRightPts.y);
    
    // Bottom left corner mark
    CGPathMoveToPoint(cropMarkPath, NULL, bottomLeftPts.x + borderLineLength, bottomLeftPts.y);
	CGPathAddLineToPoint(cropMarkPath, NULL, bottomLeftPts.x, bottomLeftPts.y);
	CGPathAddLineToPoint(cropMarkPath, NULL, bottomLeftPts.x, bottomLeftPts.y - borderLineLength);
	   
    CAShapeLayer *cropMarkLayer = [CAShapeLayer layer];
    [cropMarkLayer setBounds:overlay.bounds];
    [cropMarkLayer setPosition:overlay.center];
    [cropMarkLayer setFillColor:[[UIColor clearColor] CGColor]];
    [cropMarkLayer setStrokeColor:[[UIColor blackColor] CGColor]];
    [cropMarkLayer setLineWidth:1.0f];
    [cropMarkLayer setLineJoin:kCALineJoinBevel];
    [cropMarkLayer setLineDashPattern:
    [NSArray arrayWithObjects:[NSNumber numberWithInt:10], [NSNumber numberWithInt:5],nil]];
    [cropMarkLayer setPath:cropMarkPath];
    CGPathRelease(cropMarkPath);
    [[overlay layer] addSublayer:cropMarkLayer];
    
    return overlay;
}

- (void) cancelTakePicture {
    [self dismissCameraPreview];
}

- (void)viewWillLayoutSubviews {
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPad) {
        [self layoutForTablet];
    } else {
        [self layoutForPhone];
    }
}

- (UIImage *)imageWithColor:(UIColor *)color {
    CGRect rect = CGRectMake(0.0f, 0.0f, 1.0f, 1.0f);
    UIGraphicsBeginImageContext(rect.size);
    CGContextRef context = UIGraphicsGetCurrentContext();
    
    CGContextSetFillColorWithColor(context, [color CGColor]);
    CGContextFillRect(context, rect);
    
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    
    return image;
}

- (void)layoutForPhone {
    CGRect bounds = [[UIScreen mainScreen] bounds];
    
    NSInteger height = bounds.size.height;
    NSInteger width = bounds.size.width - kHeaderHeightPhone;
    NSInteger logoSizeW = _logoPanel.image.size.width;
    NSInteger logoSizeH =_logoPanel.image.size.height;
    
    CGFloat textPadding = 10;
    
    _captureButton.frame = CGRectMake((((width) / 2) - (kCaptureButtonWidthPhone / 2)) + textPadding,
                                      bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                      kCaptureButtonWidthPhone,
                                      kCaptureButtonHeightPhone);
    _captureButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _cancelButton.frame = CGRectMake(-textPadding,
                                     bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                     kCaptureButtonWidthPhone - 5,
                                     kCaptureButtonHeightPhone);
    _cancelButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _buttonPanel.frame = CGRectMake(0,
                                    CGRectGetMinY(_captureButton.frame) - kCaptureButtonVerticalInsetPhone,
                                    bounds.size.width - kHeaderHeightPhone,
                                    kCaptureButtonHeightPhone + (kCaptureButtonVerticalInsetPhone * 2));
    
    _headerPanel.frame = CGRectMake(width, 0, kHeaderHeightPhone, height);
    
    _logoPanel.frame = CGRectMake((kHeaderHeightPhone - logoSizeH)/ 2, (height - logoSizeW) / 2 , logoSizeH, logoSizeW);
    
    _topFramePanel.frame = CGRectMake(0, 0, width, kFrameBorderSizePhone);
    
    _leftFramePanel.frame = CGRectMake(0, kFrameBorderSizePhone, kFrameBorderSizePhone, height - kFrameBorderSizePhone  - _buttonPanel.frame.size.height);
    
    _titlePanel.frame = CGRectMake(width - kFrameBorderSizePhone, kFrameBorderSizePhone, kFrameBorderSizePhone, height -kFrameBorderSizePhone - _buttonPanel.frame.size.height);
    
    previewLayer.frame = CGRectMake(kFrameBorderSizePhone, kFrameBorderSizePhone, width - (kFrameBorderSizePhone * 2), height - kFrameBorderSizePhone - kCaptureButtonHeightPhone - (kCaptureButtonVerticalInsetPhone * 2));
}

- (void)layoutForPhoneWithShortScreen {
    CGRect bounds = [[UIScreen mainScreen] bounds];
    
    NSInteger height = bounds.size.height;
    NSInteger width = bounds.size.width - kHeaderHeightPhone;
    NSInteger logoSizeW = _logoPanel.image.size.width;
    NSInteger logoSizeH =_logoPanel.image.size.height;
    
    _captureButton.frame = CGRectMake((((width) / 2) - (kCaptureButtonWidthPhone / 2)),
                                      bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                      kCaptureButtonWidthPhone,
                                      kCaptureButtonHeightPhone);
    _captureButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _cancelButton.frame = CGRectMake(-20,
                                     bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                     kCaptureButtonWidthPhone - 5,
                                     kCaptureButtonHeightPhone);
    _cancelButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _buttonPanel.frame = CGRectMake(0,
                                    CGRectGetMinY(_captureButton.frame) - kCaptureButtonVerticalInsetPhone,
                                    bounds.size.width - kHeaderHeightPhone,
                                    kCaptureButtonHeightPhone + (kCaptureButtonVerticalInsetPhone * 2));
    
    _headerPanel.frame = CGRectMake(width, 0, kHeaderHeightPhone, height);
    
    _logoPanel.frame = CGRectMake((kHeaderHeightPhone - logoSizeH)/ 2, (height - logoSizeW) / 2 , logoSizeH, logoSizeW);
    
    _topFramePanel.frame = CGRectMake(0, 0, width, kFrameBorderSizePhone);
    
    _leftFramePanel.frame = CGRectMake(0, kFrameBorderSizePhone, kFrameBorderSizePhone, height - kFrameBorderSizePhone  - _buttonPanel.frame.size.height);
    
    _titlePanel.frame = CGRectMake(width - kFrameBorderSizePhone, kFrameBorderSizePhone, kFrameBorderSizePhone, height -kFrameBorderSizePhone - _buttonPanel.frame.size.height);
    
    previewLayer.frame = CGRectMake(kFrameBorderSizePhone, kFrameBorderSizePhone, width - (kFrameBorderSizePhone * 2), height - kFrameBorderSizePhone - kCaptureButtonHeightPhone - (kCaptureButtonVerticalInsetPhone * 2));
    
    
}

- (void)layoutForPhoneWithTallScreen {
    CGRect bounds = [[UIScreen mainScreen] bounds];
    
    NSInteger height = bounds.size.height;
    NSInteger width = bounds.size.width - kHeaderHeightPhone;
    NSInteger logoSizeW = _logoPanel.image.size.width;
    NSInteger logoSizeH =_logoPanel.image.size.height;
    
    _captureButton.frame = CGRectMake((((width) / 2) - (kCaptureButtonWidthPhone / 2)),
                                      bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                      kCaptureButtonWidthPhone,
                                      kCaptureButtonHeightPhone);
    _captureButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _cancelButton.frame = CGRectMake(-20,
                                     bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                     kCaptureButtonWidthPhone - 5,
                                     kCaptureButtonHeightPhone);
    _cancelButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _buttonPanel.frame = CGRectMake(0,
                                    CGRectGetMinY(_captureButton.frame) - kCaptureButtonVerticalInsetPhone,
                                    bounds.size.width - kHeaderHeightPhone,
                                    kCaptureButtonHeightPhone + (kCaptureButtonVerticalInsetPhone * 2));
    
    _headerPanel.frame = CGRectMake(width, 0, kHeaderHeightPhone, height);
    
    _logoPanel.frame = CGRectMake((kHeaderHeightPhone - logoSizeH)/ 2, (height - logoSizeW) / 2 , logoSizeH, logoSizeW);
    
    _topFramePanel.frame = CGRectMake(0, 0, width, kFrameBorderSizePhone);
    
    _leftFramePanel.frame = CGRectMake(0, kFrameBorderSizePhone, kFrameBorderSizePhone, height - kFrameBorderSizePhone  - _buttonPanel.frame.size.height);
    
    _titlePanel.frame = CGRectMake(width - kFrameBorderSizePhone, kFrameBorderSizePhone, kFrameBorderSizePhone, height -kFrameBorderSizePhone - _buttonPanel.frame.size.height);
    
    previewLayer.frame = CGRectMake(kFrameBorderSizePhone, kFrameBorderSizePhone, width - (kFrameBorderSizePhone * 2), height - kFrameBorderSizePhone - kCaptureButtonHeightPhone - (kCaptureButtonVerticalInsetPhone * 2));
}

- (void)layoutForTablet {
    CGRect bounds = [[UIScreen mainScreen] bounds];
    
    NSInteger height = bounds.size.height;
    NSInteger width = bounds.size.width - kHeaderHeightPhone;
    NSInteger logoSizeW = _logoPanel.image.size.width;
    NSInteger logoSizeH =_logoPanel.image.size.height;
    
    _captureButton.frame = CGRectMake((((width) / 2) - (kCaptureButtonWidthPhone / 2)),
                                      bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                      kCaptureButtonWidthPhone,
                                      kCaptureButtonHeightPhone);
    _captureButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _cancelButton.frame = CGRectMake(-20,
                                     bounds.size.height - kCaptureButtonHeightPhone - kCaptureButtonVerticalInsetPhone,
                                     kCaptureButtonWidthPhone - 5,
                                     kCaptureButtonHeightPhone);
    _cancelButton.layer.cornerRadius = kCaptureButtonRadiusPhone;
    
    _buttonPanel.frame = CGRectMake(0,
                                    CGRectGetMinY(_captureButton.frame) - kCaptureButtonVerticalInsetPhone,
                                    bounds.size.width - kHeaderHeightPhone,
                                    kCaptureButtonHeightPhone + (kCaptureButtonVerticalInsetPhone * 2));
    
    _headerPanel.frame = CGRectMake(width, 0, kHeaderHeightPhone, height);
    
    _logoPanel.frame = CGRectMake((kHeaderHeightPhone - logoSizeH)/ 2, (height - logoSizeW) / 2 , logoSizeH, logoSizeW);
    
    _topFramePanel.frame = CGRectMake(0, 0, width, kFrameBorderSizePhone);
    
    _leftFramePanel.frame = CGRectMake(0, kFrameBorderSizePhone, kFrameBorderSizePhone, height - kFrameBorderSizePhone  - _buttonPanel.frame.size.height);
    
    _titlePanel.frame = CGRectMake(width - kFrameBorderSizePhone, kFrameBorderSizePhone, kFrameBorderSizePhone, height -kFrameBorderSizePhone - _buttonPanel.frame.size.height);
    
    previewLayer.frame = CGRectMake(kFrameBorderSizePhone, kFrameBorderSizePhone, width - (kFrameBorderSizePhone * 2), height - kFrameBorderSizePhone - kCaptureButtonHeightPhone - (kCaptureButtonVerticalInsetPhone * 2));
}

- (void)viewDidLoad {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0), ^{
        for (AVCaptureDevice *device in [AVCaptureDevice devices]) {
            if ([device hasMediaType:AVMediaTypeVideo] && [device position] == AVCaptureDevicePositionBack) {
                _rearCamera = device;
            }
        }
        AVCaptureDeviceInput *cameraInput = [AVCaptureDeviceInput deviceInputWithDevice:_rearCamera error:nil];
        [_captureSession addInput:cameraInput];
        _stillImageOutput = [[AVCaptureStillImageOutput alloc] init];
        [_captureSession addOutput:_stillImageOutput];
        [_captureSession startRunning];
        dispatch_async(dispatch_get_main_queue(), ^{
            [_activityIndicator stopAnimating];
        });
    });
}

- (void)viewWillAppear:(BOOL)animated {
    [super viewWillAppear:animated];
    [[UIApplication sharedApplication] setStatusBarHidden:YES];
}

- (void)viewWillDisappear:(BOOL)animated {
    [super viewWillDisappear:animated];
    [[UIApplication sharedApplication] setStatusBarHidden:NO];
}

- (BOOL)prefersStatusBarHidden {
    return YES;
}

- (NSUInteger)supportedInterfaceOrientations {
    return UIInterfaceOrientationMaskPortrait;
}

- (UIInterfaceOrientation)preferredInterfaceOrientationForPresentation {
    return UIInterfaceOrientationPortrait;
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)orientation {
    return orientation == UIDeviceOrientationPortrait;
}

- (void)dismissCameraPreview {
    [self dismissViewControllerAnimated:YES completion:nil];
}

- (void)takePictureWaitingForCameraToFocus {
    if (_rearCamera.adjustingFocus) {
        [_rearCamera addObserver:self forKeyPath:@"adjustingFocus" options:NSKeyValueObservingOptionNew context:nil];
    } else {
        [self takePicture];
    }
}

- (void)observeValueForKeyPath:(NSString*)keyPath ofObject:(id)object change:(NSDictionary*)change context:(void*)context {
    if ([keyPath isEqualToString:@"adjustingFocus"] && !_rearCamera.adjustingFocus) {
        [_rearCamera removeObserver:self forKeyPath:@"adjustingFocus"];
        [self takePicture];
    }
}

- (void)takePicture {
    [_activityIndicator startAnimating];
    AVCaptureConnection *videoConnection = [self videoConnectionToOutput:_stillImageOutput];
    [_stillImageOutput captureStillImageAsynchronouslyFromConnection:videoConnection completionHandler:^(CMSampleBufferRef imageSampleBuffer, NSError *error) {
        NSData *imageData = [AVCaptureStillImageOutput jpegStillImageNSDataRepresentation:imageSampleBuffer];
        _callback([UIImage imageWithData:imageData]);
    }];
}

- (AVCaptureConnection*)videoConnectionToOutput:(AVCaptureOutput*)output {
    for (AVCaptureConnection *connection in output.connections) {
        for (AVCaptureInputPort *port in [connection inputPorts]) {
            if ([[port mediaType] isEqual:AVMediaTypeVideo]) {
                return connection;
            }
        }
    }
    return nil;
}

@end
