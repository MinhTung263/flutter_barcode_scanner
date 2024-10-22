import 'dart:async';

import 'package:flutter/services.dart';

/// Scan mode which is either QR code or BARCODE
enum ScanMode { QR, BARCODE, DEFAULT }

/// Provides access to the barcode scanner.
///
/// This class is an interface between the native Android and iOS classes and a
/// Flutter project.
class FlutterBarcodeScanner {
  static const MethodChannel _channel =
      MethodChannel('flutter_barcode_scanner');

  static const EventChannel _eventChannel =
      EventChannel('flutter_barcode_scanner_receiver');

  static Stream? _onBarcodeReceiver;

  /// Scan with the camera until a barcode is identified, then return.
  ///
  /// Shows a scan line with [lineColor] over a scan window. A flash icon is
  /// displayed if [isShowFlashIcon] is true. The text of the cancel button can
  static Future<String> scanBarcode({
    Color? lineColor,
    bool? isShowFlashIcon,
    ScanMode? scanMode,
  }) async {
    final lineColorHex = lineColor != null
        ? '#${lineColor.value.toRadixString(16).padLeft(8, '0').substring(2)}'
        : '#ff6666'; // Default color if none is provided
    // Pass params to the plugin
    final Map params = <String, dynamic>{
      'lineColor': lineColorHex,
      'isShowFlashIcon': isShowFlashIcon ?? true,
      'isContinuousScan': false,
      'scanMode': scanMode?.index ?? ScanMode.BARCODE.index
    };

    /// Get barcode scan result
    final barcodeResult =
        await _channel.invokeMethod('scanBarcode', params) ?? '';
    return barcodeResult;
  }

  /// Returns a continuous stream of barcode scans until the user cancels the
  /// operation.
  ///
  /// Shows a scan line with [lineColor] over a scan window. A flash icon is
  /// displayed if [isShowFlashIcon] is true. The text of the cancel button can
  /// detected barcode strings.
  static Stream? getBarcodeStreamReceiver({
    Color? lineColor,
    bool? isShowFlashIcon,
    ScanMode? scanMode,
  }) {
    final lineColorHex = lineColor != null
        ? '#${lineColor.value.toRadixString(16).padLeft(8, '0').substring(2)}'
        : '#ff6666'; // Default color if none is provided
    // Pass params to the plugin
    Map params = <String, dynamic>{
      'lineColor': lineColorHex,
      'isShowFlashIcon': isShowFlashIcon ?? true,
      'isContinuousScan': true,
      'scanMode': scanMode?.index ?? ScanMode.BARCODE.index,
    };

    // Invoke method to open camera, and then create an event channel which will
    // return a stream
    _channel.invokeMethod('scanBarcode', params);
    _onBarcodeReceiver ??= _eventChannel.receiveBroadcastStream();
    return _onBarcodeReceiver;
  }
}
