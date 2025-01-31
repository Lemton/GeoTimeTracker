// filepath: /d:/App-Projects/gofencing/lib/src/map_feature/street_view_widget.dart
import 'package:flutter/material.dart';

class StreetViewWidget extends StatelessWidget {
  const StreetViewWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Container(
      color: Colors.blue,
      child: Center(
        child: Text(
          'Street View',
          style: TextStyle(color: Colors.white, fontSize: 24),
        ),
      ),
    );
  }
}