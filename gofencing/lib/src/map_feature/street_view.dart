import 'package:flutter/material.dart';
import 'dart:math';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

class StreetViewWidget extends StatefulWidget {
  const StreetViewWidget({super.key});

  @override
  _StreetViewWidgetState createState() => _StreetViewWidgetState();
}

class _StreetViewWidgetState extends State<StreetViewWidget> {
  final Random _random = Random();
  late String _streetViewUrl;
  late LatLng _streetViewPosition;

  @override
  void initState() {
    super.initState();
    _loadRandomStreetView();
  }

  void _loadRandomStreetView() {
    // Beispielkoordinaten für zufällige Positionen
    final List<LatLng> positions = [
      LatLng(37.7749, -122.4194), // San Francisco
      LatLng(40.7128, -74.0060),  // New York
      LatLng(34.0522, -118.2437), // Los Angeles
    ];

    _streetViewPosition = positions[_random.nextInt(positions.length)];
    _streetViewUrl = 'https://maps.googleapis.com/maps/api/streetview?size=400x400&location=${_streetViewPosition.latitude},${_streetViewPosition.longitude}&fov=80&heading=70&pitch=0&key=${dotenv.env['GOOGLE_MAPS_API_KEY']}';
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Expanded(
              child: Image.network(_streetViewUrl),
            ),
            Text(
              'Street View',
              style: TextStyle(color: Colors.black, fontSize: 24),
            ),
          ],
        ),
      ),
    );
  }
}