import 'dart:convert';
import 'package:flutter_test/flutter_test.dart';
import 'package:mockito/mockito.dart';
import 'package:http/http.dart' as http;
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:gofencing/src/services/location_services.dart';

// Mock class for http.Client
class MockClient extends Mock implements http.Client {}

void main() {
  group('LocationService', () {
    late LocationService locationService;
    late MockClient mockClient;

    setUp(() {
      locationService = LocationService();
      mockClient = MockClient();
    });

    test('should load random Street View and update currentStreetViewLocation', () async {
      final currentLocation = LatLng(37.7749, -122.4194);
      final metadataResponse = {
        'status': 'OK',
        'location': {'lat': 37.7750, 'lng': -122.4195}
      };

      // Mock the HTTP response for the metadata request
      when(mockClient.get(any as Uri)).thenAnswer((_) async => http.Response(json.encode(metadataResponse), 200));

      // Inject the mock client into the LocationService
      await locationService.loadRandomStreetView(currentLocation);

      // Verify that the currentStreetViewLocation is updated correctly
      expect(locationService.currentStreetViewLocation, isNotNull);
      expect(locationService.currentStreetViewLocation!.latitude, 37.7750);
      expect(locationService.currentStreetViewLocation!.longitude, -122.4195);
    });

    test('should trigger Street View update', () async {
      final currentLocation = LatLng(37.7749, -122.4194);
      final metadataResponse = {
        'status': 'OK',
        'location': {'lat': 37.7750, 'lng': -122.4195}
      };

      // Mock the HTTP response for the metadata request
      when(mockClient.get(any as Uri)).thenAnswer((_) async => http.Response(json.encode(metadataResponse), 200));

      // Inject the mock client into the LocationService
      await locationService.triggerStreetViewUpdate();

      // Verify that the currentStreetViewLocation is updated correctly
      expect(locationService.currentStreetViewLocation, isNotNull);
      expect(locationService.currentStreetViewLocation!.latitude, 37.7750);
      expect(locationService.currentStreetViewLocation!.longitude, -122.4195);
    });
  });
}