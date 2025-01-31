// filepath: /d:/App-Projects/gofencing/lib/src/app.dart
import 'package:flutter/material.dart';
import 'settings/settings_controller.dart';
import 'settings/settings_view.dart';
import 'map_feature/home_map_view.dart';

class MyApp extends StatelessWidget {
  const MyApp({
    super.key,
    required this.settingsController,
  });

  final SettingsController settingsController;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: settingsController,
      builder: (BuildContext context, Widget? child) {
        return MaterialApp(
          title: 'Flutter Demo',
          theme: ThemeData(),
          darkTheme: ThemeData.dark(),
          themeMode: settingsController.themeMode,
          initialRoute: HomeMapView.routeName,
          routes: {
            HomeMapView.routeName: (context) => const HomeMapView(),
            SettingsView.routeName: (context) => SettingsView(controller: settingsController),
          },
        );
      },
    );
  }
}