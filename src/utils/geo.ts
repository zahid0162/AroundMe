import { GeoPoint, DistanceUnit } from '../types/aroundme';

const EARTH_RADIUS_METERS = 6_371_000;

export function haversineMeters(from: GeoPoint, to: GeoPoint): number {
  const toRad = (deg: number) => (deg * Math.PI) / 180;
  const dLat = toRad(to.lat - from.lat);
  const dLng = toRad(to.lng - from.lng);
  const lat1 = toRad(from.lat);
  const lat2 = toRad(to.lat);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return EARTH_RADIUS_METERS * c;
}

export function formatDistance(
  meters: number,
  unit: DistanceUnit,
  isArabic: boolean
): string {
  if (unit === 'metric') {
    if (meters < 1000) {
      const rounded = Math.round(meters / 10) * 10;
      return isArabic ? `${rounded} م` : `${rounded} m`;
    }
    const km = (meters / 1000).toFixed(1);
    return isArabic ? `${km} كم` : `${km} km`;
  }

  const feet = meters * 3.28084;
  if (feet < 1000) {
    const rounded = Math.round(feet / 50) * 50;
    return isArabic ? `${rounded} قدم` : `${rounded} ft`;
  }
  const miles = (meters / 1609.344).toFixed(1);
  return isArabic ? `${miles} ميل` : `${miles} mi`;
}
