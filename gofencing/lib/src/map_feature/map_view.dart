import 'dart:async';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:gofencing/src/services/location_services.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:flutter/services.dart' show rootBundle, ByteData;
import 'dart:ui' as ui;

class MapViewWidget extends StatefulWidget {
  const MapViewWidget({super.key});

  @override
  _MapViewWidgetState createState() => _MapViewWidgetState();
}

class _MapViewWidgetState extends State<MapViewWidget> {
  late GoogleMapController mapController;
  LatLng _currentPosition = LatLng(0, 0);
  String _mapStyle = '';
  final Set<Marker> _markers = {};
  BitmapDescriptor? _customIcon;
  final LocationService _locationService = LocationService();

  @override
  void initState() {
    super.initState();
    _loadMapStyle();
    _loadCustomMarker().then((_) {
      _locationService.startLocationUpdates();
      _locationService.locationStream.listen((LatLng position) {
        setState(() {
          _currentPosition = position;
          _markers.add(
            Marker(
              markerId: MarkerId('currentLocation'),
              position: _currentPosition,
              icon: _customIcon ?? BitmapDescriptor.defaultMarker,
              infoWindow: InfoWindow(title: 'Current Location'),
            ),
          );
          mapController.animateCamera(CameraUpdate.newLatLng(_currentPosition));
        });
      });
    });
  }

  @override
  void dispose() {
    _locationService.stopLocationUpdates();
    super.dispose();
  }

  Future<void> _loadMapStyle() async {
    _mapStyle = await rootBundle.loadString('assets/map_style.json');
  }

  Future<void> _loadCustomMarker() async {
    final ByteData data = await rootBundle.load('assets/custom_marker.png');
    final Uint8List bytes = data.buffer.asUint8List();
    final ui.Codec codec = await ui.instantiateImageCodec(bytes, targetWidth: 100);
    final ui.FrameInfo fi = await codec.getNextFrame();
    final ByteData? byteData = await fi.image.toByteData(format: ui.ImageByteFormat.png);
    final Uint8List resizedBytes = byteData!.buffer.asUint8List();
    _customIcon = BitmapDescriptor.fromBytes(resizedBytes);
  }

  void _onMapCreated(GoogleMapController controller) {
    mapController = controller;
    mapController.setMapStyle(_mapStyle);
  }

  @override
  Widget build(BuildContext context) {
    return GoogleMap(
      onMapCreated: _onMapCreated,
      initialCameraPosition: CameraPosition(
        target: _currentPosition,
        zoom: 15.0,
      ),
      myLocationEnabled: false,
      myLocationButtonEnabled: true,
      markers: _markers,
    );
  }
}