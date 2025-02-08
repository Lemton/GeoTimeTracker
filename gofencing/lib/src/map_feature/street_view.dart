import 'package:flutter/material.dart';
import 'package:gofencing/src/services/location_services.dart';


class StreetViewWidget extends StatefulWidget {
  const StreetViewWidget({super.key});

  @override
  _StreetViewWidgetState createState() => _StreetViewWidgetState();
}

class _StreetViewWidgetState extends State<StreetViewWidget> {
  final LocationService _locationService = LocationService();
  String _streetViewUrl = '';

  @override
  void initState() {
    super.initState();
    _locationService.startLocationUpdates();
    _locationService.streetViewStream.listen((String url) {
      setState(() {
        _streetViewUrl = url;
      });
    });
  }

  @override
  void dispose() {
    _locationService.stopLocationUpdates();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.white,
      child: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            if (_streetViewUrl.isNotEmpty)
              Expanded(
                child: Image.network(
                  _streetViewUrl,
                  width: double.infinity,
                  fit: BoxFit.cover,
                ),
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