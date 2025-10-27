import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart' hide Color;

Future<String> svgAssetToBase64(String assetPath) async {
  final bytes = await rootBundle.load(assetPath);
  final base64Str = base64Encode(bytes.buffer.asUint8List());
  return 'data:image/svg+xml;base64,$base64Str';
}

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
        : '#ff6666';

    const pkg = 'packages/flutter_barcode_scanner/assets/icons/';

    // Helper để convert asset sang base64, trả về null nếu path rỗng
    Future<String?> _toBase64(String? assetPath) async {
      if (assetPath == null || assetPath.isEmpty) return null;
      return await svgAssetToBase64(assetPath);
    }

    // Load tất cả icon cùng lúc
    final results = await Future.wait([
      _toBase64('${pkg}flashOff.svg'),
      _toBase64('${pkg}flashOn.svg'),
      _toBase64('${pkg}cancel-button.svg'),
      _toBase64('${pkg}camera-switch.svg'),
    ]);

    final params = <String, dynamic>{
      'lineColor': lineColorHex,
      'isShowFlashIcon': isShowFlashIcon ?? true,
      'isContinuousScan': false,
      'scanMode': scanMode?.index ?? ScanMode.BARCODE.index,
      'flashOffIcon': results[0],
      'flashOnIcon': results[1],
      'cancelButtonIcon': results[2],
      'cameraSwitchIcon': results[3],
    };

    return await _channel.invokeMethod('scanBarcode', params) ?? '';
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
