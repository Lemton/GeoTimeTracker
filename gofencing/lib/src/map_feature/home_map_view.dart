import 'package:flutter/material.dart';
import 'street_view.dart';
import 'map_view.dart';
import '../settings/settings_view.dart';

class HomeMapView extends StatefulWidget {
  const HomeMapView({super.key});

  static const routeName = '/';

  @override
  _HomeMapViewState createState() => _HomeMapViewState();
}

class _HomeMapViewState extends State<HomeMapView> {
  bool isGreen = true;

  void toggleLight() {
    setState(() {
      isGreen = !isGreen;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Go touch grass and visit me!'),
        actions: [
          IconButton(
            icon: const Icon(Icons.settings),
            onPressed: () {
              Navigator.restorablePushNamed(context, SettingsView.routeName);
            },
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: StreetViewWidget(),
          ),
          Expanded(
            child: MapViewWidget(),
          ),
        ],
      ),
      bottomNavigationBar: BottomAppBar(
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('Nutzername', style: TextStyle(fontSize: 18)),
              IconButton(
                icon: Icon(
                  Icons.circle,
                  color: isGreen ? Colors.green : Colors.red,
                ),
                onPressed: toggleLight,
              ),
            ],
          ),
        ),
      ),
    );
  }
}