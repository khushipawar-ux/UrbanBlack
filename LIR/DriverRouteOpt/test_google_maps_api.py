"""
test_google_maps_api.py — Test if Google Maps API key is working

Usage:
    python test_google_maps_api.py
    
This will test:
1. API key validity
2. Directions API
3. Distance Matrix API
"""

import sys
from pathlib import Path
from dotenv import load_dotenv
import os

# Load environment variables
load_dotenv()

import googlemaps
from googlemaps.exceptions import ApiError, TransportError, Timeout

# Test coordinates in Pune
ORIGIN = (18.5204, 73.8567)  # Pune center
DESTINATION = (18.5362, 73.8935)  # ~5km away


def test_google_maps_api():
    """Test if Google Maps API key is working."""
    
    print("=" * 70)
    print("Google Maps API — Functionality Test")
    print("=" * 70)
    
    api_key = os.getenv("GOOGLE_MAPS_API_KEY", "").strip()
    
    # Check 1: API Key Present
    print("\n[1/4] Checking API Key...")
    if not api_key:
        print("  ✗ No API key found in .env")
        print("    → Add GOOGLE_MAPS_API_KEY to .env file")
        return False
    
    if api_key == "YOUR_API_KEY_HERE":
        print("  ✗ API key is still placeholder: YOUR_API_KEY_HERE")
        print("    → Replace with actual key from Google Cloud Console")
        return False
    
    print(f"  ✓ API key found (starts with: {api_key[:10]}...)")
    
    # Check 2: Create client
    print("\n[2/4] Creating Google Maps client...")
    try:
        client = googlemaps.Client(key=api_key, timeout=5)
        print("  ✓ Client created successfully")
    except Exception as e:
        print(f"  ✗ Failed to create client: {e}")
        return False
    
    # Check 3: Test Directions API
    print("\n[3/4] Testing Directions API...")
    try:
        response = client.directions(
            origin=ORIGIN,
            destination=DESTINATION,
            mode="driving"
        )
        if response:
            route = response[0]
            distance = route['legs'][0]['distance']['value']  # meters
            duration = route['legs'][0]['duration']['value']  # seconds
            distance_km = distance / 1000
            duration_min = duration / 60
            
            print(f"  ✓ Directions API working!")
            print(f"    Distance: {distance_km:.2f} km")
            print(f"    Duration: {duration_min:.1f} minutes")
        else:
            print("  ✗ Directions API returned empty response")
            return False
    except ApiError as e:
        print(f"  ✗ API Error: {e}")
        print("    → Check API limits or service quota")
        return False
    except Exception as e:
        print(f"  ✗ Failed: {e}")
        return False
    
    # Check 4: Test Distance Matrix API
    print("\n[4/4] Testing Distance Matrix API...")
    try:
        response = client.distance_matrix(
            origins=[ORIGIN],
            destinations=[DESTINATION],
            mode="driving"
        )
        if response['rows']:
            element = response['rows'][0]['elements'][0]
            if element['status'] == 'OK':
                distance = element['distance']['value']
                duration = element['duration']['value']
                distance_km = distance / 1000
                duration_min = duration / 60
                
                print(f"  ✓ Distance Matrix API working!")
                print(f"    Distance: {distance_km:.2f} km")
                print(f"    Duration: {duration_min:.1f} minutes")
            else:
                print(f"  ✗ Invalid response: {element['status']}")
                return False
        else:
            print("  ✗ Distance Matrix returned empty")
            return False
    except ApiError as e:
        print(f"  ✗ API Error: {e}")
        return False
    except Exception as e:
        print(f"  ✗ Failed: {e}")
        return False
    
    # Success!
    print("\n" + "=" * 70)
    print("✓ ALL TESTS PASSED — Google Maps API is working properly!")
    print("=" * 70)
    print("\nYour API key is valid and all services are accessible.")
    print("You can now run: python main.py")
    print("=" * 70)
    return True


def get_api_key_instructions():
    """Print instructions for getting Google Maps API key."""
    print("\n" + "=" * 70)
    print("How to Get a Google Maps API Key")
    print("=" * 70)
    print("""
1. Go to Google Cloud Console:
   https://console.cloud.google.com/

2. Create a new project (or select existing):
   - Click "Select a Project" at top
   - Click "New Project"
   - Enter name: "Urban Black"
   - Click "Create"

3. Enable APIs:
   - Search for "Maps SDK for Android"
   - Click "Enable"
   - Search for "Directions API"
   - Click "Enable"
   - Search for "Distance Matrix API"
   - Click "Enable"

4. Create API Key:
   - Go to "Credentials" in left menu
   - Click "Create Credentials" > "API Key"
   - Copy the key

5. Restrict your key (Optional but recommended):
   - Click on your API key
   - Under "Application restrictions" set to "HTTP referrers"
   - Under "API restrictions" select "Maps APIs" and enable:
     * Directions API
     * Distance Matrix API
     * Maps SDK for Android (if using mobile)

6. Add to .env:
   GOOGLE_MAPS_API_KEY=AIza...your...key...here

7. Test it:
   python test_google_maps_api.py

""")
    print("=" * 70)


if __name__ == "__main__":
    try:
        success = test_google_maps_api()
        if not success:
            # Show instructions on failure
            get_api_key_instructions()
            sys.exit(1)
    except KeyboardInterrupt:
        print("\n\nTest cancelled")
        sys.exit(0)
