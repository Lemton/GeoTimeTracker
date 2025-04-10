import 'dart:async';
import 'dart:convert';
import 'dart:math';
import 'package:geolocator/geolocator.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;

class LocationService {
  StreamSubscription<Position>? _positionStreamSubscription;
  final StreamController<LatLng> _locationController = StreamController<LatLng>.broadcast();
  final StreamController<String> _streetViewController = StreamController<String>.broadcast();
  
  LatLng? _currentStreetViewLocation;

  Stream<LatLng> get locationStream => _locationController.stream;
  Stream<String> get streetViewStream => _streetViewController.stream;

  LatLng? get currentStreetViewLocation => _currentStreetViewLocation;

  void startLocationUpdates() {
    const LocationSettings locationSettings = LocationSettings(
      accuracy: LocationAccuracy.high,
      distanceFilter: 10, // Update distance in meters
    );

    _positionStreamSubscription = Geolocator.getPositionStream(locationSettings: locationSettings)
        .listen((Position position) {
      final currentLocation = LatLng(position.latitude, position.longitude);
      _locationController.add(currentLocation);

      // Check if the current location is near the current Street View location
      updateStreetView(currentLocation);
    });

    // Trigger initial Street View update
    updateStreetView();
  }

  void updateStreetView([LatLng? currentLocation]) {
    if (currentLocation != null) {
      if (_currentStreetViewLocation != null && _calculateDistance(_currentStreetViewLocation!, currentLocation) < 10) {
        loadRandomStreetView(currentLocation);
      }
    } else {
      _locationController.stream.first.then((location) {
        loadRandomStreetView(location);
      });
    }
  }

  void stopLocationUpdates() {
    _positionStreamSubscription?.cancel();
    _locationController.close();
    _streetViewController.close();
  }

  Future<void> loadRandomStreetView(LatLng currentLocation) async {
    final Random random = Random();
    final List<LatLng> positions = [
      LatLng(currentLocation.latitude + random.nextDouble() * 0.01 - 0.005, currentLocation.longitude + random.nextDouble() * 0.01 - 0.005),
      LatLng(currentLocation.latitude + random.nextDouble() * 0.01 - 0.005, currentLocation.longitude + random.nextDouble() * 0.01 - 0.005),
      LatLng(currentLocation.latitude + random.nextDouble() * 0.01 - 0.005, currentLocation.longitude + random.nextDouble() * 0.01 - 0.005),
    ];

    final selectedPosition = positions[random.nextInt(positions.length)];
    final streetViewUrl = 'https://maps.googleapis.com/maps/api/streetview?size=400x400&location=${selectedPosition.latitude},${selectedPosition.longitude}&fov=80&heading=70&pitch=0&key=${dotenv.env['GOOGLE_MAPS_API_KEY']}';

    // Fetch Street View metadata
    final metadataUrl = 'https://maps.googleapis.com/maps/api/streetview/metadata?location=${selectedPosition.latitude},${selectedPosition.longitude}&key=${dotenv.env['GOOGLE_MAPS_API_KEY']}';
    final response = await http.get(Uri.parse(metadataUrl));
    if (response.statusCode == 200) {
      final metadata = json.decode(response.body);
      if (metadata['status'] == 'OK') {
        _currentStreetViewLocation = LatLng(metadata['location']['lat'], metadata['location']['lng']);
      }
    }

    _streetViewController.add(streetViewUrl);
  }

  Future<void> triggerStreetViewUpdate() async {
    final location = await _locationController.stream.first;
    await loadRandomStreetView(location);
  }

  double _calculateDistance(LatLng start, LatLng end) {
    const double earthRadius = 6371000; // meters
    final double dLat = _degreesToRadians(end.latitude - start.latitude);
    final double dLng = _degreesToRadians(end.longitude - start.longitude);
    final double a = 
      sin(dLat / 2) * sin(dLat / 2) +
      cos(_degreesToRadians(start.latitude)) * cos(_degreesToRadians(end.latitude)) *
      sin(dLng / 2) * sin(dLng / 2);
    final double c = 2 * atan2(sqrt(a), sqrt(1 - a));
    return earthRadius * c;
  }

  double _degreesToRadians(double degrees) {
    return degrees * pi / 180;
  }
}