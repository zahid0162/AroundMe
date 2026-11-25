export type PlaceCategoryId =
  | 'restaurants'
  | 'cafes'
  | 'pharmacies'
  | 'parks'
  | 'museums'
  | 'bakeries'
  | 'supermarkets'
  | 'mosques';

export type SortOption = 'recommended' | 'nearest' | 'highest_rated' | 'most_reviewed';
export type SheetDetent = 'collapsed' | 'half' | 'expanded';
export type ExploreViewMode = 'map' | 'list';
export type AppTab = 'explore' | 'search' | 'saved' | 'profile';
export type ThemeMode = 'light' | 'dark';
export type Language = 'en' | 'ar';
export type DistanceUnit = 'metric' | 'imperial';

export interface GeoPoint {
  lat: number;
  lng: number;
}

export interface LocationContext {
  id: string;
  labelEn: string;
  labelAr: string;
  coordinates: GeoPoint;
  isCurrentLocation: boolean;
}

export interface Place {
  id: string;
  nameEn: string;
  nameAr: string;
  category: PlaceCategoryId;
  categoryLabelEn: string;
  categoryLabelAr: string;
  addressEn: string;
  addressAr: string;
  location: GeoPoint;
  rating?: number;
  reviewCount?: number;
  isOpenNow?: boolean;
  priceSymbol?: string;
  phoneNumber?: string;
  websiteUrl?: string;
  hoursSummaryEn?: string;
  hoursSummaryAr?: string;
  attributesEn: string[];
  attributesAr: string[];
  summaryEn?: string;
  summaryAr?: string;
  imageUrl?: string;
}

export interface SavedCollection {
  id: string;
  nameEn: string;
  nameAr: string;
  placeIds: string[];
}
