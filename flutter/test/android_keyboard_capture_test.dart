import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_hbb/mobile/android_keyboard_capture.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();
  final calls = <MethodCall>[];

  setUp(() {
    calls.clear();
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(AndroidKeyboardCapture.channel, (call) async {
      calls.add(call);
      return true;
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(AndroidKeyboardCapture.channel, null);
  });

  Future<void> update({
    bool enabled = true,
    bool foreground = true,
    bool focused = true,
    bool connected = true,
    bool keyboardPermitted = true,
    bool viewOnly = false,
  }) =>
      AndroidKeyboardCapture.update(
        enabled: enabled,
        foreground: foreground,
        focused: focused,
        connected: connected,
        keyboardPermitted: keyboardPermitted,
        viewOnly: viewOnly,
      );

  test('captures only for an enabled, focused controlling session', () async {
    await update();
    expect(calls.single.method, 'setEnabled');
    expect(calls.single.arguments, true);
    await update(enabled: false);
    expect(calls.last.arguments, false);
  });

  test('releases capture when focus, connection or control access is lost',
      () async {
    await update();
    await update(foreground: false);
    await update(focused: false);
    await update(connected: false);
    await update(keyboardPermitted: false);
    await update(viewOnly: true);
    expect(calls.skip(1).map((call) => call.arguments), everyElement(false));
  });

  test('checks service availability without enabling capture', () async {
    expect(await AndroidKeyboardCapture.serviceOpen, true);
    expect(calls.single.method, 'isServiceOpen');
  });
}
