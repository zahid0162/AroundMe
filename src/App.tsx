import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowLeft,
  ArrowRight,
  Bookmark,
  ChevronDown,
  ChevronUp,
  Clock,
  Compass,
  Globe,
  History,
  List,
  LocateFixed,
  Map as MapIcon,
  MapPin,
  Navigation,
  Phone,
  Search,
  Share2,
  Star,
  User,
  X,
} from 'lucide-react';
import {
  AppTab,
  DistanceUnit,
  ExploreViewMode,
  Language,
  LocationContext,
  Place,
  PlaceCategoryId,
  SavedCollection,
  SheetDetent,
  SortOption,
  ThemeMode,
} from './types/aroundme';
import {
  CATEGORY_OPTIONS,
  INITIAL_COLLECTIONS,
  SAMPLE_LOCATIONS,
  SAMPLE_PLACES,
} from './data/samplePlaces';
import { formatDistance, haversineMeters } from './utils/geo';

function PlaceThumbnail({
  place,
  isArabic,
  sizeClass = 'w-20 h-20',
}: {
  place: Place;
  isArabic: boolean;
  sizeClass?: string;
}) {
  const [failed, setFailed] = useState(false);

  if (place.imageUrl && !failed) {
    return (
      <img
        src={place.imageUrl}
        alt={isArabic ? place.nameAr : place.nameEn}
        referrerPolicy="no-referrer"
        onError={() => setFailed(true)}
        className={`${sizeClass} rounded-xl object-cover shrink-0 bg-slate-200 dark:bg-slate-800`}
      />
    );
  }

  return (
    <div
      className={`${sizeClass} rounded-xl shrink-0 flex items-center justify-center bg-gradient-to-br from-[#d9f3f1] to-[#fef3c7] dark:from-[#063d3d] dark:to-[#452408] text-[#063d3d] dark:text-[#d9f3f1] font-semibold text-sm`}
    >
      <MapPin className="w-5 h-5" />
    </div>
  );
}

function PlaceCard({
  place,
  referencePoint,
  distanceUnit,
  isArabic,
  isSaved,
  isSelected,
  onSelect,
  onToggleSave,
}: {
  place: Place;
  referencePoint: LocationContext;
  distanceUnit: DistanceUnit;
  isArabic: boolean;
  isSaved: boolean;
  isSelected: boolean;
  onSelect: () => void;
  onToggleSave: () => void;
}) {
  const distanceMeters = haversineMeters(referencePoint.coordinates, place.location);
  const name = isArabic ? place.nameAr : place.nameEn;
  const catLabel = isArabic ? place.categoryLabelAr : place.categoryLabelEn;

  return (
    <div
      role="button"
      tabIndex={0}
      onClick={onSelect}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          onSelect();
        }
      }}
      className={`w-full text-start p-3 rounded-2xl border transition-colors cursor-pointer bg-white dark:bg-[#0f171a] ${
        isSelected
          ? 'border-[#0d6e6e] dark:border-[#4fd1c5]'
          : 'border-slate-200 dark:border-slate-800 hover:border-slate-300 dark:hover:border-slate-700'
      }`}
    >
      <div className="flex items-center gap-3">
        <PlaceThumbnail place={place} isArabic={isArabic} />
        <div className="min-w-0 flex-1 space-y-1">
          <h3 className="text-[15px] font-semibold text-slate-900 dark:text-slate-100 truncate">
            {name}
          </h3>
          <div className="flex flex-wrap items-center gap-1.5 text-xs text-slate-600 dark:text-slate-400 tabular-nums">
            {place.rating !== undefined && (
              <>
                <span className="inline-flex items-center gap-1 font-semibold text-slate-900 dark:text-slate-100">
                  <Star className="w-3.5 h-3.5 fill-[#d96b27] text-[#d96b27] dark:fill-[#f59e0b] dark:text-[#f59e0b]" />
                  {place.rating.toFixed(1)}
                  {place.reviewCount !== undefined ? ` (${place.reviewCount})` : ''}
                </span>
                <span aria-hidden="true">·</span>
              </>
            )}
            <span className="truncate">{catLabel}</span>
          </div>
          <div className="flex flex-wrap items-center gap-1.5 text-xs tabular-nums">
            {place.isOpenNow !== undefined && (
              <span
                className={
                  place.isOpenNow
                    ? 'font-medium text-emerald-700 dark:text-emerald-400'
                    : 'font-medium text-rose-700 dark:text-rose-400'
                }
              >
                {place.isOpenNow
                  ? isArabic
                    ? 'مفتوح الآن'
                    : 'Open now'
                  : isArabic
                  ? 'مغلق'
                  : 'Closed'}
              </span>
            )}
            {place.isOpenNow !== undefined && (
              <span aria-hidden="true" className="text-slate-400">
                ·
              </span>
            )}
            <span className="text-slate-600 dark:text-slate-400">
              {formatDistance(distanceMeters, distanceUnit, isArabic)}
            </span>
            {place.priceSymbol && (
              <>
                <span aria-hidden="true" className="text-slate-400">
                  ·
                </span>
                <span className="text-slate-600 dark:text-slate-400">{place.priceSymbol}</span>
              </>
            )}
          </div>
        </div>
        <button
          type="button"
          onClick={(e) => {
            e.stopPropagation();
            onToggleSave();
          }}
          aria-label={isSaved ? 'Remove saved place' : 'Save place'}
          className="min-h-[44px] min-w-[44px] flex items-center justify-center rounded-xl text-slate-500 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
        >
          <Bookmark
            className={`w-5 h-5 ${
              isSaved
                ? 'fill-[#0d6e6e] text-[#0d6e6e] dark:fill-[#4fd1c5] dark:text-[#4fd1c5]'
                : ''
            }`}
          />
        </button>
      </div>
    </div>
  );
}

export default function App() {
  const [activeTab, setActiveTab] = useState<AppTab>('explore');
  const [theme, setTheme] = useState<ThemeMode>('light');
  const [language, setLanguage] = useState<Language>('en');
  const [distanceUnit, setDistanceUnit] = useState<DistanceUnit>('metric');

  const [activeLocation, setActiveLocation] = useState<LocationContext>(SAMPLE_LOCATIONS[0]);
  const [locationDropdownOpen, setLocationDropdownOpen] = useState(false);

  const [selectedCategory, setSelectedCategory] = useState<PlaceCategoryId | null>(null);
  const [openNowOnly, setOpenNowOnly] = useState(false);
  const [sortOption, setSortOption] = useState<SortOption>('recommended');
  const [viewMode, setViewMode] = useState<ExploreViewMode>('map');
  const [sheetDetent, setSheetDetent] = useState<SheetDetent>('half');
  const [selectedPlaceId, setSelectedPlaceId] = useState<string | null>(SAMPLE_PLACES[0].id);
  const [detailPlaceId, setDetailPlaceId] = useState<string | null>(null);
  const [mapMoved, setMapMoved] = useState(false);

  const [searchQuery, setSearchQuery] = useState('');
  const [recentSearches, setRecentSearches] = useState<string[]>([
    'Specialty coffee',
    '24h Pharmacy',
    'Levantine dinner',
    'Parks',
  ]);

  const [collections, setCollections] = useState<SavedCollection[]>(INITIAL_COLLECTIONS);
  const [selectedCollectionId, setSelectedCollectionId] = useState<string | null>(null);
  const [feedbackMessage, setFeedbackMessage] = useState<string | null>(null);

  const isArabic = language === 'ar';

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'dark') {
      root.classList.add('dark');
    } else {
      root.classList.remove('dark');
    }
    root.dir = isArabic ? 'rtl' : 'ltr';
    root.lang = language;
  }, [theme, isArabic, language]);

  useEffect(() => {
    if (!feedbackMessage) return;
    const timer = window.setTimeout(() => setFeedbackMessage(null), 2600);
    return () => window.clearTimeout(timer);
  }, [feedbackMessage]);

  const savedPlaceIds = useMemo(() => {
    const set = new Set<string>();
    collections.forEach((col) => col.placeIds.forEach((id) => set.add(id)));
    return set;
  }, [collections]);

  const toggleSavePlace = (placeId: string) => {
    const currentlySaved = savedPlaceIds.has(placeId);
    setCollections((prev) =>
      prev.map((col) => {
        if (currentlySaved) {
          return { ...col, placeIds: col.placeIds.filter((id) => id !== placeId) };
        }
        if (col.id === 'want_to_visit') {
          return { ...col, placeIds: Array.from(new Set([...col.placeIds, placeId])) };
        }
        return col;
      })
    );
  };

  const filteredPlaces = useMemo(() => {
    const list = SAMPLE_PLACES.filter((place) => {
      if (selectedCategory && place.category !== selectedCategory) return false;
      if (openNowOnly && !place.isOpenNow) return false;
      return true;
    });

    return [...list].sort((a, b) => {
      if (sortOption === 'nearest') {
        return (
          haversineMeters(activeLocation.coordinates, a.location) -
          haversineMeters(activeLocation.coordinates, b.location)
        );
      }
      if (sortOption === 'highest_rated') {
        return (b.rating ?? 0) - (a.rating ?? 0);
      }
      if (sortOption === 'most_reviewed') {
        return (b.reviewCount ?? 0) - (a.reviewCount ?? 0);
      }
      const scoreA =
        (a.rating ?? 3.5) * 2 -
        haversineMeters(activeLocation.coordinates, a.location) / 1000;
      const scoreB =
        (b.rating ?? 3.5) * 2 -
        haversineMeters(activeLocation.coordinates, b.location) / 1000;
      return scoreB - scoreA;
    });
  }, [selectedCategory, openNowOnly, sortOption, activeLocation]);

  const searchResults = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    if (!q) return [];
    return SAMPLE_PLACES.filter(
      (p) =>
        p.nameEn.toLowerCase().includes(q) ||
        p.nameAr.includes(q) ||
        p.categoryLabelEn.toLowerCase().includes(q) ||
        p.categoryLabelAr.includes(q) ||
        p.addressEn.toLowerCase().includes(q)
    );
  }, [searchQuery]);

  const savedPlacesList = useMemo(() => {
    const activeIds = selectedCollectionId
      ? new Set(collections.find((c) => c.id === selectedCollectionId)?.placeIds ?? [])
      : savedPlaceIds;
    return SAMPLE_PLACES.filter((p) => activeIds.has(p.id));
  }, [collections, selectedCollectionId, savedPlaceIds]);

  const detailPlace = useMemo(
    () => SAMPLE_PLACES.find((p) => p.id === detailPlaceId) ?? null,
    [detailPlaceId]
  );

  const cycleSheetDetent = () => {
    setSheetDetent((prev) =>
      prev === 'collapsed' ? 'half' : prev === 'half' ? 'expanded' : 'collapsed'
    );
  };

  const navItems: Array<{
    id: AppTab;
    labelEn: string;
    labelAr: string;
    icon: React.ComponentType<{ className?: string }>;
  }> = [
    { id: 'explore', labelEn: 'Explore', labelAr: 'استكشف', icon: Compass },
    { id: 'search', labelEn: 'Search', labelAr: 'بحث', icon: Search },
    { id: 'saved', labelEn: 'Saved', labelAr: 'المحفوظات', icon: Bookmark },
    { id: 'profile', labelEn: 'Profile', labelAr: 'الملف', icon: User },
  ];

  const activeLocationLabel = isArabic ? activeLocation.labelAr : activeLocation.labelEn;

  return (
    <div className="min-h-screen flex flex-col bg-[#f8fafb] dark:bg-[#090e11] text-slate-900 dark:text-slate-100">
      {/* Desktop Header Bar (Clean 3-Zone Contract) */}
      <header className="hidden lg:flex items-center justify-between gap-8 px-6 h-16 border-b border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a] shrink-0 z-30">
        <button
          type="button"
          onClick={() => {
            setDetailPlaceId(null);
            setActiveTab('explore');
          }}
          className="text-lg font-bold tracking-tight text-[#0d6e6e] dark:text-[#4fd1c5] whitespace-nowrap shrink-0"
        >
          AroundMe
        </button>

        <nav className="flex items-center gap-6">
          {navItems.map((item) => {
            const active = activeTab === item.id && !detailPlace;
            return (
              <button
                key={item.id}
                type="button"
                onClick={() => {
                  setDetailPlaceId(null);
                  setActiveTab(item.id);
                }}
                className={`text-sm font-medium whitespace-nowrap shrink-0 pb-1 border-b-2 transition-colors ${
                  active
                    ? 'border-[#0d6e6e] dark:border-[#4fd1c5] text-slate-900 dark:text-white'
                    : 'border-transparent text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                }`}
              >
                {isArabic ? item.labelAr : item.labelEn}
              </button>
            );
          })}
        </nav>

        <div className="flex items-center gap-2 shrink-0">
          <button
            type="button"
            onClick={() => setLanguage((l) => (l === 'en' ? 'ar' : 'en'))}
            className="px-3 py-1.5 text-xs font-medium rounded-lg border border-slate-200 dark:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800 whitespace-nowrap shrink-0 transition-colors"
          >
            {isArabic ? 'English' : 'العربية (RTL)'}
          </button>
          <button
            type="button"
            onClick={() => setTheme((t) => (t === 'light' ? 'dark' : 'light'))}
            className="px-3 py-1.5 text-xs font-medium rounded-lg bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] whitespace-nowrap shrink-0 transition-colors"
          >
            {theme === 'light'
              ? isArabic
                ? 'الوضع الداكن'
                : 'Dark Mode'
              : isArabic
              ? 'الوضع الفاتح'
              : 'Light Mode'}
          </button>
        </div>
      </header>

      {/* Main Content Shell */}
      <main className="flex-1 flex flex-col min-h-0 relative pb-16 lg:pb-0">
        {detailPlace ? (
          /* Place Details Screen */
          <div className="max-w-4xl w-full mx-auto px-4 py-5 space-y-5">
            <div className="flex items-center justify-between">
              <button
                type="button"
                onClick={() => setDetailPlaceId(null)}
                className="min-h-[44px] inline-flex items-center gap-2 px-3 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a] text-sm font-medium"
              >
                {isArabic ? <ArrowRight className="w-4 h-4" /> : <ArrowLeft className="w-4 h-4" />}
                <span>{isArabic ? 'رجوع' : 'Back'}</span>
              </button>
              <button
                type="button"
                onClick={() => toggleSavePlace(detailPlace.id)}
                className="min-h-[44px] inline-flex items-center gap-2 px-4 rounded-xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a] text-sm font-medium"
              >
                <Bookmark
                  className={`w-4 h-4 ${
                    savedPlaceIds.has(detailPlace.id)
                      ? 'fill-[#0d6e6e] text-[#0d6e6e] dark:fill-[#4fd1c5] dark:text-[#4fd1c5]'
                      : ''
                  }`}
                />
                <span>
                  {savedPlaceIds.has(detailPlace.id)
                    ? isArabic
                      ? 'محفوظ'
                      : 'Saved'
                    : isArabic
                    ? 'حفظ'
                    : 'Save'}
                </span>
              </button>
            </div>

            <div className="rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a]">
              {detailPlace.imageUrl ? (
                <div className="relative h-56 sm:h-72 w-full">
                  <img
                    src={detailPlace.imageUrl}
                    alt={isArabic ? detailPlace.nameAr : detailPlace.nameEn}
                    referrerPolicy="no-referrer"
                    className="w-full h-full object-cover"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/20 to-transparent" />
                  <div className="absolute bottom-4 start-4 end-4 text-white">
                    <p className="text-xs font-medium opacity-90">
                      {isArabic ? detailPlace.categoryLabelAr : detailPlace.categoryLabelEn}
                    </p>
                    <h1 className="text-2xl font-bold mt-0.5">
                      {isArabic ? detailPlace.nameAr : detailPlace.nameEn}
                    </h1>
                  </div>
                </div>
              ) : (
                <div className="p-6 bg-gradient-to-br from-[#d9f3f1] to-[#fef3c7] dark:from-[#063d3d] dark:to-[#1e293b]">
                  <p className="text-xs font-medium text-[#063d3d] dark:text-[#d9f3f1]">
                    {isArabic ? detailPlace.categoryLabelAr : detailPlace.categoryLabelEn}
                  </p>
                  <h1 className="text-2xl font-bold mt-1">
                    {isArabic ? detailPlace.nameAr : detailPlace.nameEn}
                  </h1>
                </div>
              )}

              <div className="p-5 space-y-4">
                <div className="flex flex-wrap items-center gap-2 text-sm tabular-nums text-slate-600 dark:text-slate-300">
                  {detailPlace.rating !== undefined && (
                    <span className="inline-flex items-center gap-1 font-semibold text-slate-900 dark:text-white">
                      <Star className="w-4 h-4 fill-[#d96b27] text-[#d96b27]" />
                      {detailPlace.rating.toFixed(1)} ({detailPlace.reviewCount})
                    </span>
                  )}
                  <span aria-hidden="true">·</span>
                  <span>
                    {formatDistance(
                      haversineMeters(activeLocation.coordinates, detailPlace.location),
                      distanceUnit,
                      isArabic
                    )}
                  </span>
                  {detailPlace.priceSymbol && (
                    <>
                      <span aria-hidden="true">·</span>
                      <span>{detailPlace.priceSymbol}</span>
                    </>
                  )}
                </div>

                {(isArabic ? detailPlace.summaryAr : detailPlace.summaryEn) && (
                  <p className="text-sm leading-relaxed text-slate-600 dark:text-slate-300">
                    {isArabic ? detailPlace.summaryAr : detailPlace.summaryEn}
                  </p>
                )}

                <div className="flex flex-wrap gap-2">
                  <button
                    type="button"
                    onClick={() =>
                      setFeedbackMessage(
                        isArabic
                          ? 'سيتم فتح تطبيق الخرائط للاتجاهات في المرحلة 4'
                          : 'Native navigation handoff wired in Phase 4'
                      )
                    }
                    className="min-h-[44px] px-4 rounded-xl bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] text-sm font-semibold inline-flex items-center gap-2"
                  >
                    <Navigation className="w-4 h-4" />
                    <span>{isArabic ? 'الاتجاهات' : 'Directions'}</span>
                  </button>
                  {detailPlace.phoneNumber && (
                    <button
                      type="button"
                      onClick={() => setFeedbackMessage(detailPlace.phoneNumber ?? '')}
                      className="min-h-[44px] px-4 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium inline-flex items-center gap-2"
                    >
                      <Phone className="w-4 h-4" />
                      <span>{isArabic ? 'اتصال' : 'Call'}</span>
                    </button>
                  )}
                  {detailPlace.websiteUrl && (
                    <button
                      type="button"
                      onClick={() => setFeedbackMessage(detailPlace.websiteUrl ?? '')}
                      className="min-h-[44px] px-4 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium inline-flex items-center gap-2"
                    >
                      <Globe className="w-4 h-4" />
                      <span>{isArabic ? 'الموقع' : 'Website'}</span>
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() =>
                      setFeedbackMessage(
                        isArabic ? 'تم نسخ رابط المشاركة' : 'Place share link ready'
                      )
                    }
                    className="min-h-[44px] px-4 rounded-xl border border-slate-200 dark:border-slate-700 text-sm font-medium inline-flex items-center gap-2"
                  >
                    <Share2 className="w-4 h-4" />
                    <span>{isArabic ? 'مشاركة' : 'Share'}</span>
                  </button>
                </div>

                <div className="pt-3 border-t border-slate-200 dark:border-slate-800 space-y-2.5 text-sm">
                  <div className="flex items-start gap-2.5">
                    <MapPin className="w-4 h-4 text-[#0d6e6e] dark:text-[#4fd1c5] mt-0.5 shrink-0" />
                    <span>{isArabic ? detailPlace.addressAr : detailPlace.addressEn}</span>
                  </div>
                  {(isArabic ? detailPlace.hoursSummaryAr : detailPlace.hoursSummaryEn) && (
                    <div className="flex items-start gap-2.5">
                      <Clock className="w-4 h-4 text-[#0d6e6e] dark:text-[#4fd1c5] mt-0.5 shrink-0" />
                      <span>
                        {isArabic ? detailPlace.hoursSummaryAr : detailPlace.hoursSummaryEn}
                      </span>
                    </div>
                  )}
                </div>

                {(isArabic ? detailPlace.attributesAr : detailPlace.attributesEn).length > 0 && (
                  <div className="pt-2 text-xs text-slate-600 dark:text-slate-400">
                    {(isArabic ? detailPlace.attributesAr : detailPlace.attributesEn).join(' · ')}
                  </div>
                )}
              </div>
            </div>
          </div>
        ) : activeTab === 'explore' ? (
          /* Explore Screen — Map-first with 3-State Sheet on Mobile & Split Map+List on Desktop */
          <div className="flex-1 flex flex-col lg:grid lg:grid-cols-12 min-h-[calc(100vh-4rem)] relative">
            {/* List / Controls Panel (Desktop Left, Mobile 3-State Bottom Sheet or Full List) */}
            <section
              className={`${
                viewMode === 'list'
                  ? 'flex-1 lg:col-span-5 flex flex-col bg-[#f8fafb] dark:bg-[#090e11] z-20'
                  : 'hidden lg:flex lg:col-span-5 flex-col border-e border-slate-200 dark:border-slate-800 bg-[#f8fafb] dark:bg-[#090e11]'
              }`}
            >
              <div className="p-4 space-y-3 border-b border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a]">
                <div className="flex items-center justify-between gap-2">
                  <div className="relative">
                    <button
                      type="button"
                      onClick={() => setLocationDropdownOpen((v) => !v)}
                      className="min-h-[40px] inline-flex items-center gap-2 px-3 py-1.5 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-medium"
                    >
                      <MapPin className="w-3.5 h-3.5 text-[#0d6e6e] dark:text-[#4fd1c5]" />
                      <span>{activeLocationLabel}</span>
                      <ChevronDown className="w-3.5 h-3.5" />
                    </button>
                    {locationDropdownOpen && (
                      <div className="absolute top-full mt-1 start-0 w-56 rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0f171a] shadow-lg py-1 z-40">
                        {SAMPLE_LOCATIONS.map((loc) => (
                          <button
                            key={loc.id}
                            type="button"
                            onClick={() => {
                              setActiveLocation(loc);
                              setLocationDropdownOpen(false);
                              setMapMoved(false);
                            }}
                            className="w-full text-start px-3 py-2 text-xs hover:bg-slate-100 dark:hover:bg-slate-800"
                          >
                            {isArabic ? loc.labelAr : loc.labelEn}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  <button
                    type="button"
                    onClick={() => setViewMode((m) => (m === 'map' ? 'list' : 'map'))}
                    className="lg:hidden min-h-[40px] inline-flex items-center gap-1.5 px-3 rounded-xl border border-slate-200 dark:border-slate-700 text-xs font-medium"
                  >
                    {viewMode === 'map' ? (
                      <>
                        <List className="w-3.5 h-3.5" />
                        <span>{isArabic ? 'قائمة' : 'List'}</span>
                      </>
                    ) : (
                      <>
                        <MapIcon className="w-3.5 h-3.5" />
                        <span>{isArabic ? 'خريطة' : 'Map'}</span>
                      </>
                    )}
                  </button>
                </div>

                <button
                  type="button"
                  onClick={() => setActiveTab('search')}
                  className="w-full min-h-[44px] flex items-center gap-2.5 px-3.5 rounded-xl border border-slate-200 dark:border-slate-700 bg-[#f8fafb] dark:bg-[#090e11] text-sm text-slate-500 dark:text-slate-400"
                >
                  <Search className="w-4 h-4 text-[#0d6e6e] dark:text-[#4fd1c5]" />
                  <span>
                    {isArabic
                      ? 'ابحث عن مطاعم، مقاهي، صيدليات…'
                      : 'Search places, cafes, pharmacies…'}
                  </span>
                </button>

                <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-0.5">
                  <button
                    type="button"
                    onClick={() => setSelectedCategory(null)}
                    className={`min-h-[36px] px-3 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
                      selectedCategory === null
                        ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d]'
                        : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                    }`}
                  >
                    {isArabic ? 'الكل' : 'All'}
                  </button>
                  {CATEGORY_OPTIONS.map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() =>
                        setSelectedCategory((curr) => (curr === cat.id ? null : cat.id))
                      }
                      className={`min-h-[36px] px-3 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 transition-colors ${
                        selectedCategory === cat.id
                          ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d]'
                          : 'bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      {isArabic ? cat.labelAr : cat.labelEn}
                    </button>
                  ))}
                </div>

                <div className="flex items-center justify-between gap-2 pt-1">
                  <button
                    type="button"
                    onClick={() => setOpenNowOnly((v) => !v)}
                    className={`min-h-[34px] px-3 rounded-lg text-xs font-medium border transition-colors ${
                      openNowOnly
                        ? 'border-[#0d6e6e] bg-[#d9f3f1] text-[#063d3d] dark:border-[#4fd1c5] dark:bg-[#063d3d] dark:text-[#d9f3f1]'
                        : 'border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-300'
                    }`}
                  >
                    {isArabic ? 'مفتوح الآن' : 'Open now'}
                  </button>

                  <select
                    aria-label="Sort places"
                    value={sortOption}
                    onChange={(e) => setSortOption(e.target.value as SortOption)}
                    className="min-h-[34px] px-2.5 rounded-lg text-xs border border-slate-200 dark:border-slate-700 bg-white dark:bg-[#0f171a]"
                  >
                    <option value="recommended">
                      {isArabic ? 'الموصى به' : 'Recommended'}
                    </option>
                    <option value="nearest">{isArabic ? 'الأقرب' : 'Nearest'}</option>
                    <option value="highest_rated">
                      {isArabic ? 'الأعلى تقييماً' : 'Highest rated'}
                    </option>
                    <option value="most_reviewed">
                      {isArabic ? 'الأكثر مراجعة' : 'Most reviewed'}
                    </option>
                  </select>
                </div>
              </div>

              <div className="flex-1 overflow-y-auto p-4 space-y-3">
                <p className="text-xs font-medium text-slate-500 dark:text-slate-400 tabular-nums">
                  {isArabic
                    ? `${filteredPlaces.length} مكان قريب`
                    : `${filteredPlaces.length} places nearby`}
                </p>
                {filteredPlaces.map((place) => (
                  <PlaceCard
                    key={place.id}
                    place={place}
                    referencePoint={activeLocation}
                    distanceUnit={distanceUnit}
                    isArabic={isArabic}
                    isSaved={savedPlaceIds.has(place.id)}
                    isSelected={selectedPlaceId === place.id}
                    onSelect={() => {
                      setSelectedPlaceId(place.id);
                      setDetailPlaceId(place.id);
                    }}
                    onToggleSave={() => toggleSavePlace(place.id)}
                  />
                ))}
              </div>
            </section>

            {/* Interactive Map Canvas */}
            <section
              className={`${
                viewMode === 'list' ? 'hidden lg:block lg:col-span-7' : 'flex-1 lg:col-span-7'
              } relative bg-[#e6ecee] dark:bg-[#131c21] overflow-hidden min-h-[420px]`}
            >
              {/* Stylized Vector Road Grid (Phase 2 replaces with Native Google Maps SDKs) */}
              <svg
                className="absolute inset-0 w-full h-full opacity-50 pointer-events-none"
                xmlns="http://www.w3.org/2000/svg"
              >
                <defs>
                  <pattern id="grid" width="64" height="64" patternUnits="userSpaceOnUse">
                    <path
                      d="M 64 0 L 0 0 0 64"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="0.75"
                      className="text-slate-400/40 dark:text-slate-600/40"
                    />
                  </pattern>
                </defs>
                <rect width="100%" height="100%" fill="url(#grid)" />
                <path
                  d="M -40 190 Q 260 150 920 240"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="12"
                  className="text-white/80 dark:text-slate-800"
                />
                <path
                  d="M 280 -20 Q 340 280 420 760"
                  fill="none"
                  stroke="currentColor"
                  strokeWidth="10"
                  className="text-white/80 dark:text-slate-800"
                />
              </svg>

              {/* Mobile Floating Top Bar */}
              <div className="lg:hidden absolute top-3 start-3 end-3 z-20 space-y-2">
                <div className="flex items-center justify-between gap-2">
                  <button
                    type="button"
                    onClick={() => setLocationDropdownOpen((v) => !v)}
                    className="min-h-[40px] inline-flex items-center gap-1.5 px-3 rounded-xl bg-white/95 dark:bg-[#0f171a]/95 backdrop-blur-md shadow-sm text-xs font-medium"
                  >
                    <span className="font-bold text-[#0d6e6e] dark:text-[#4fd1c5]">AroundMe</span>
                    <span aria-hidden="true">·</span>
                    <span className="truncate max-w-[140px]">{activeLocationLabel}</span>
                    <ChevronDown className="w-3.5 h-3.5" />
                  </button>

                  <div className="flex items-center gap-1.5">
                    <button
                      type="button"
                      onClick={() => setActiveTab('search')}
                      aria-label="Search"
                      className="min-h-[40px] min-w-[40px] flex items-center justify-center rounded-xl bg-white/95 dark:bg-[#0f171a]/95 backdrop-blur-md shadow-sm"
                    >
                      <Search className="w-4 h-4" />
                    </button>
                    <button
                      type="button"
                      onClick={() => setViewMode('list')}
                      className="min-h-[40px] inline-flex items-center gap-1.5 px-3 rounded-xl bg-white/95 dark:bg-[#0f171a]/95 backdrop-blur-md shadow-sm text-xs font-medium"
                    >
                      <List className="w-3.5 h-3.5" />
                      <span>{isArabic ? 'قائمة' : 'List'}</span>
                    </button>
                  </div>
                </div>

                <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
                  <button
                    type="button"
                    onClick={() => setSelectedCategory(null)}
                    className={`min-h-[34px] px-3 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 shadow-sm ${
                      selectedCategory === null
                        ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d]'
                        : 'bg-white/95 dark:bg-[#0f171a]/95'
                    }`}
                  >
                    {isArabic ? 'الكل' : 'All'}
                  </button>
                  {CATEGORY_OPTIONS.map((cat) => (
                    <button
                      key={cat.id}
                      type="button"
                      onClick={() =>
                        setSelectedCategory((curr) => (curr === cat.id ? null : cat.id))
                      }
                      className={`min-h-[34px] px-3 rounded-lg text-xs font-medium whitespace-nowrap shrink-0 shadow-sm ${
                        selectedCategory === cat.id
                          ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d]'
                          : 'bg-white/95 dark:bg-[#0f171a]/95'
                      }`}
                    >
                      {isArabic ? cat.labelAr : cat.labelEn}
                    </button>
                  ))}
                </div>
              </div>

              {/* Search This Area Control */}
              <div className="absolute top-24 lg:top-4 left-1/2 -translate-x-1/2 z-20 flex items-center gap-2">
                {mapMoved ? (
                  <button
                    type="button"
                    onClick={() => setMapMoved(false)}
                    className="min-h-[38px] px-4 rounded-xl bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] text-xs font-semibold shadow-md"
                  >
                    {isArabic ? 'ابحث في هذه المنطقة' : 'Search this area'}
                  </button>
                ) : (
                  <button
                    type="button"
                    onClick={() => setMapMoved(true)}
                    className="min-h-[36px] px-3.5 rounded-xl bg-white/90 dark:bg-[#0f171a]/90 backdrop-blur-md text-xs font-medium border border-slate-200 dark:border-slate-700 shadow-sm"
                  >
                    {isArabic ? 'محاكاة تحريك الخريطة' : 'Simulate Map Pan'}
                  </button>
                )}
              </div>

              {/* Current Location Marker */}
              <div
                className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-5 h-5 rounded-full bg-[#0d6e6e]/25 flex items-center justify-center pointer-events-none"
                aria-hidden="true"
              >
                <div className="w-3 h-3 rounded-full bg-[#0d6e6e] dark:bg-[#4fd1c5] border-2 border-white" />
              </div>

              {/* Place Markers */}
              {filteredPlaces.map((place, index) => {
                const offsetX = ((place.location.lng - SAMPLE_LOCATIONS[0].coordinates.lng) * 2600) % 160;
                const offsetY = ((SAMPLE_LOCATIONS[0].coordinates.lat - place.location.lat) * 2600) % 150;
                const isSelected = selectedPlaceId === place.id;
                return (
                  <button
                    key={place.id}
                    type="button"
                    onClick={() => setSelectedPlaceId(place.id)}
                    style={{
                      left: `calc(50% + ${offsetX}px)`,
                      top: `calc(46% + ${offsetY}px)`,
                    }}
                    className={`absolute -translate-x-1/2 -translate-y-1/2 z-10 flex flex-col items-center transition-transform ${
                      isSelected ? 'scale-110 z-20' : 'hover:scale-105'
                    }`}
                  >
                    <span
                      className={`px-2.5 py-1.5 rounded-xl shadow-md text-xs font-semibold inline-flex items-center gap-1 border ${
                        isSelected
                          ? 'bg-[#d96b27] text-white border-[#d96b27]'
                          : 'bg-white dark:bg-[#0f171a] text-slate-900 dark:text-slate-100 border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      <MapPin className="w-3.5 h-3.5 shrink-0" />
                      {(isSelected || index < 3) && (
                        <span className="max-w-[110px] truncate">
                          {isArabic ? place.nameAr : place.nameEn}
                        </span>
                      )}
                    </span>
                  </button>
                );
              })}

              {/* My Location FAB */}
              <button
                type="button"
                onClick={() => {
                  setActiveLocation(SAMPLE_LOCATIONS[0]);
                  setMapMoved(false);
                }}
                aria-label="Return to current location"
                className="hidden lg:flex absolute bottom-6 end-6 w-11 h-11 items-center justify-center rounded-full bg-white dark:bg-[#0f171a] border border-slate-200 dark:border-slate-700 shadow-md text-[#0d6e6e] dark:text-[#4fd1c5]"
              >
                <LocateFixed className="w-5 h-5" />
              </button>

              {/* Mobile 3-State Draggable-Style Bottom Sheet */}
              <div
                className={`lg:hidden absolute bottom-0 start-0 end-0 z-20 rounded-t-3xl bg-white dark:bg-[#0f171a] border-t border-slate-200 dark:border-slate-800 shadow-xl transition-all duration-200 flex flex-col ${
                  sheetDetent === 'collapsed'
                    ? 'h-[88px]'
                    : sheetDetent === 'half'
                    ? 'h-[45%]'
                    : 'h-[78%]'
                }`}
              >
                <button
                  type="button"
                  onClick={cycleSheetDetent}
                  className="w-full pt-2 pb-2 px-4 flex flex-col items-center shrink-0"
                >
                  <span className="w-10 h-1 rounded-full bg-slate-300 dark:bg-slate-700 mb-2" />
                  <div className="w-full flex items-center justify-between">
                    <span className="text-sm font-semibold tabular-nums">
                      {isArabic
                        ? `${filteredPlaces.length} مكان قريب`
                        : `${filteredPlaces.length} places nearby`}
                    </span>
                    {sheetDetent === 'expanded' ? (
                      <ChevronDown className="w-4 h-4 text-slate-500" />
                    ) : (
                      <ChevronUp className="w-4 h-4 text-slate-500" />
                    )}
                  </div>
                </button>

                {sheetDetent !== 'collapsed' && (
                  <div className="flex-1 overflow-y-auto px-4 pb-4 space-y-2.5">
                    {filteredPlaces.map((place) => (
                      <PlaceCard
                        key={place.id}
                        place={place}
                        referencePoint={activeLocation}
                        distanceUnit={distanceUnit}
                        isArabic={isArabic}
                        isSaved={savedPlaceIds.has(place.id)}
                        isSelected={selectedPlaceId === place.id}
                        onSelect={() => setDetailPlaceId(place.id)}
                        onToggleSave={() => toggleSavePlace(place.id)}
                      />
                    ))}
                  </div>
                )}
              </div>
            </section>
          </div>
        ) : activeTab === 'search' ? (
          /* Search Screen */
          <div className="max-w-3xl w-full mx-auto px-4 py-5 space-y-5">
            <div className="relative">
              <Search className="w-4 h-4 text-[#0d6e6e] dark:text-[#4fd1c5] absolute top-1/2 -translate-y-1/2 start-3.5" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={
                  isArabic
                    ? 'ابحث عن نشاط تجاري، فئة، عنوان، أو معلم…'
                    : 'Search businesses, categories, addresses, landmarks…'
                }
                className="w-full min-h-[48px] ps-10 pe-10 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a] text-sm focus:outline-none focus:border-[#0d6e6e] dark:focus:border-[#4fd1c5]"
              />
              {searchQuery && (
                <button
                  type="button"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear search"
                  className="absolute top-1/2 -translate-y-1/2 end-2 min-h-[40px] min-w-[40px] flex items-center justify-center text-slate-400"
                >
                  <X className="w-4 h-4" />
                </button>
              )}
            </div>

            {!searchQuery.trim() ? (
              <div className="space-y-3">
                <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">
                  {isArabic ? 'عمليات البحث الأخيرة' : 'Recent Searches'}
                </h2>
                <div className="divide-y divide-slate-200 dark:divide-slate-800 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a]">
                  {recentSearches.map((item) => (
                    <button
                      key={item}
                      type="button"
                      onClick={() => {
                        setSearchQuery(item);
                        setRecentSearches((prev) =>
                          [item, ...prev.filter((r) => r !== item)].slice(0, 6)
                        );
                      }}
                      className="w-full min-h-[48px] px-4 flex items-center gap-3 text-sm text-start hover:bg-slate-50 dark:hover:bg-slate-800/50"
                    >
                      <History className="w-4 h-4 text-slate-400 shrink-0" />
                      <span>{item}</span>
                    </button>
                  ))}
                </div>
              </div>
            ) : searchResults.length === 0 ? (
              <p className="text-sm text-slate-500 dark:text-slate-400">
                {isArabic ? 'لا توجد نتائج مطابقة.' : 'No matching places found.'}
              </p>
            ) : (
              <div className="space-y-3">
                {searchResults.map((place) => (
                  <PlaceCard
                    key={place.id}
                    place={place}
                    referencePoint={activeLocation}
                    distanceUnit={distanceUnit}
                    isArabic={isArabic}
                    isSaved={savedPlaceIds.has(place.id)}
                    isSelected={false}
                    onSelect={() => setDetailPlaceId(place.id)}
                    onToggleSave={() => toggleSavePlace(place.id)}
                  />
                ))}
              </div>
            )}
          </div>
        ) : activeTab === 'saved' ? (
          /* Saved Places Screen */
          <div className="max-w-3xl w-full mx-auto px-4 py-5 space-y-5">
            <h1 className="text-xl font-bold">
              {isArabic ? 'الأماكن المحفوظة' : 'Saved Places'}
            </h1>
            <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pb-1">
              <button
                type="button"
                onClick={() => setSelectedCollectionId(null)}
                className={`min-h-[38px] px-3.5 rounded-xl text-xs font-medium whitespace-nowrap shrink-0 ${
                  selectedCollectionId === null
                    ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d]'
                    : 'bg-white dark:bg-[#0f171a] border border-slate-200 dark:border-slate-800'
                }`}
              >
                {isArabic
                  ? `الكل (${savedPlaceIds.size})`
                  : `All Saved (${savedPlaceIds.size})`}
              </button>
              {collections.map((col) => (
                <button
                  key={col.id}
                  type="button"
                  onClick={() =>
                    setSelectedCollectionId((curr) => (curr === col.id ? null : col.id))
                  }
                  className={`min-h-[38px] px-3.5 rounded-xl text-xs font-medium whitespace-nowrap shrink-0 tabular-nums ${
                    selectedCollectionId === col.id
                      ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d]'
                      : 'bg-white dark:bg-[#0f171a] border border-slate-200 dark:border-slate-800'
                  }`}
                >
                  {isArabic ? col.nameAr : col.nameEn} ({col.placeIds.length})
                </button>
              ))}
            </div>

            {savedPlacesList.length === 0 ? (
              <div className="p-6 rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a] text-sm text-slate-500 dark:text-slate-400">
                {isArabic
                  ? 'لا توجد أماكن محفوظة في هذه المجموعة بعد.'
                  : 'No saved places in this collection yet.'}
              </div>
            ) : (
              <div className="space-y-3">
                {savedPlacesList.map((place) => (
                  <PlaceCard
                    key={place.id}
                    place={place}
                    referencePoint={activeLocation}
                    distanceUnit={distanceUnit}
                    isArabic={isArabic}
                    isSaved={true}
                    isSelected={false}
                    onSelect={() => setDetailPlaceId(place.id)}
                    onToggleSave={() => toggleSavePlace(place.id)}
                  />
                ))}
              </div>
            )}
          </div>
        ) : (
          /* Profile & Settings Screen */
          <div className="max-w-2xl w-full mx-auto px-4 py-5 space-y-6">
            <div>
              <h1 className="text-xl font-bold text-[#0d6e6e] dark:text-[#4fd1c5]">
                AroundMe
              </h1>
              <p className="text-sm text-slate-500 dark:text-slate-400 mt-0.5">
                {isArabic ? 'اكتشف ما حولك' : "Discover What's Around You"}
              </p>
            </div>

            <div className="rounded-2xl border border-slate-200 dark:border-slate-800 bg-white dark:bg-[#0f171a] p-5 space-y-5">
              <div className="space-y-2">
                <h2 className="text-sm font-semibold">
                  {isArabic ? 'المظهر' : 'Appearance'}
                </h2>
                <div className="flex gap-2">
                  {(['light', 'dark'] as ThemeMode[]).map((mode) => (
                    <button
                      key={mode}
                      type="button"
                      onClick={() => setTheme(mode)}
                      className={`min-h-[40px] px-4 rounded-xl text-xs font-medium border ${
                        theme === mode
                          ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] border-transparent'
                          : 'border-slate-200 dark:border-slate-700'
                      }`}
                    >
                      {mode === 'light'
                        ? isArabic
                          ? 'فاتح'
                          : 'Light'
                        : isArabic
                        ? 'داكن'
                        : 'Dark'}
                    </button>
                  ))}
                </div>
              </div>

              <div className="space-y-2">
                <h2 className="text-sm font-semibold">
                  {isArabic ? 'اللغة' : 'Language'}
                </h2>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setLanguage('en')}
                    className={`min-h-[40px] px-4 rounded-xl text-xs font-medium border ${
                      language === 'en'
                        ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] border-transparent'
                        : 'border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    English (LTR)
                  </button>
                  <button
                    type="button"
                    onClick={() => setLanguage('ar')}
                    className={`min-h-[40px] px-4 rounded-xl text-xs font-medium border ${
                      language === 'ar'
                        ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] border-transparent'
                        : 'border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    العربية (RTL)
                  </button>
                </div>
              </div>

              <div className="space-y-2">
                <h2 className="text-sm font-semibold">
                  {isArabic ? 'وحدة المسافة' : 'Distance Units'}
                </h2>
                <div className="flex gap-2">
                  <button
                    type="button"
                    onClick={() => setDistanceUnit('metric')}
                    className={`min-h-[40px] px-4 rounded-xl text-xs font-medium border ${
                      distanceUnit === 'metric'
                        ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] border-transparent'
                        : 'border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    {isArabic ? 'متري (م / كم)' : 'Metric (m / km)'}
                  </button>
                  <button
                    type="button"
                    onClick={() => setDistanceUnit('imperial')}
                    className={`min-h-[40px] px-4 rounded-xl text-xs font-medium border ${
                      distanceUnit === 'imperial'
                        ? 'bg-[#0d6e6e] dark:bg-[#4fd1c5] text-white dark:text-[#063d3d] border-transparent'
                        : 'border-slate-200 dark:border-slate-700'
                    }`}
                  >
                    {isArabic ? 'إمبراطوري (ميل)' : 'Imperial (ft / mi)'}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {feedbackMessage && (
          <div className="fixed bottom-20 lg:bottom-6 left-1/2 -translate-x-1/2 z-50 px-4 py-2.5 rounded-xl bg-slate-900 dark:bg-white text-white dark:text-slate-900 text-xs font-medium shadow-lg">
            {feedbackMessage}
          </div>
        )}
      </main>

      {/* Mobile Bottom Navigation Bar */}
      <nav className="lg:hidden fixed bottom-0 left-0 right-0 z-40 h-16 grid grid-cols-4 items-center bg-white/95 dark:bg-[#0f171a]/95 backdrop-blur-md border-t border-slate-200 dark:border-slate-800">
        {navItems.map((item) => {
          const Icon = item.icon;
          const active = activeTab === item.id && !detailPlace;
          return (
            <button
              key={item.id}
              type="button"
              onClick={() => {
                setDetailPlaceId(null);
                setActiveTab(item.id);
              }}
              className={`min-h-[44px] flex flex-col items-center justify-center gap-1 ${
                active
                  ? 'text-[#0d6e6e] dark:text-[#4fd1c5]'
                  : 'text-slate-500 dark:text-slate-400'
              }`}
            >
              <Icon className="w-5 h-5" />
              <span className="text-[11px] font-medium">
                {isArabic ? item.labelAr : item.labelEn}
              </span>
            </button>
          );
        })}
      </nav>
    </div>
  );
}
