import 'package:flutter/services.dart';

class AndroidKeyboardCapture {
  static const channel = MethodChannel('org.rustdesk/keyboard-capture');

  static Future<bool> get serviceOpen async =>
      await channel.invokeMethod<bool>('isServiceOpen') ?? false;

  static Future<void> update({
    required bool enabled,
    required bool foreground,
    required bool focused,
    required bool connected,
    required bool keyboardPermitted,
    required bool viewOnly,
  }) async {
    await channel.invokeMethod<bool>(
        'setEnabled',
        enabled &&
            foreground &&
            focused &&
            connected &&
            keyboardPermitted &&
            !viewOnly);
  }
}
