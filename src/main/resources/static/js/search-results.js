let userLatitude = 6.3667; // Default to Cotonou
let userLongitude = 2.4333;
let map;
let userMarker;
let pharmacyMarkers = [];
let isGoogleMapsLoaded = false;

// Attendre que Google Maps soit chargé
function waitForGoogleMaps(callback) {
    if (typeof google !== 'undefined' && google.maps) {
        isGoogleMapsLoaded = true;
        callback();
    } else {
        setTimeout(() => waitForGoogleMaps(callback), 100);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    // Get query parameters
    const urlParams = new URLSearchParams(window.location.search);
    const query = urlParams.get('query');
    const lat = parseFloat(urlParams.get('lat')) || userLatitude;
    const lon = parseFloat(urlParams.get('lon')) || userLongitude;

    userLatitude = lat;
    userLongitude = lon;

    if (query) {
        document.getElementById('searchTerm').textContent = query;
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = query;
        }
    }

    // Attendre que Google Maps soit chargé avant d'initialiser
    waitForGoogleMaps(() => {
        initMap();
        if (query) {
            loadSearchResults(query);
        }
    });

    // Search functionality
    const searchInput = document.getElementById('searchInput');
    if (searchInput) {
        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                const newQuery = searchInput.value.trim();
                if (newQuery) {
                    window.location.href = `/search-results?query=${encodeURIComponent(newQuery)}&lat=${userLatitude}&lon=${userLongitude}`;
                }
            }
        });
    }
});

function initMap() {
    if (!isGoogleMapsLoaded || typeof google === 'undefined') {
        console.error('Google Maps not loaded yet');
        showMapFallback();
        return;
    }

    const initialLocation = { lat: userLatitude, lng: userLongitude };

    try {
        map = new google.maps.Map(document.getElementById('map'), {
            zoom: 13,
            center: initialLocation,
            styles: [
                {
                    featureType: 'poi',
                    elementType: 'labels',
                    stylers: [{ visibility: 'off' }]
                }
            ]
        });

        // Add user location marker
        userMarker = new google.maps.Marker({
            position: initialLocation,
            map: map,
            title: 'Votre position',
            icon: {
                path: google.maps.SymbolPath.CIRCLE,
                scale: 8,
                fillColor: '#3b82f6',
                fillOpacity: 1,
                strokeColor: '#ffffff',
                strokeWeight: 2
            }
        });
    } catch (error) {
        console.error('Error initializing map:', error);
        showMapFallback();
    }
}

function showMapFallback() {
    const mapDiv = document.getElementById('map');
    if (!mapDiv) return;
    
    mapDiv.innerHTML = `
        <div class="h-full flex flex-col items-center justify-center bg-gradient-to-br from-blue-50 to-blue-100 text-gray-700 p-6">
            <i class="fas fa-map-marked-alt text-6xl mb-4 text-blue-400"></i>
            <p class="font-bold text-lg mb-2">Carte temporairement indisponible</p>
            <p class="text-sm text-center mb-4 text-gray-600">Configuration Google Maps API requise</p>
            <a href="https://www.google.com/maps?q=${userLatitude},${userLongitude}" 
               target="_blank"
               class="bg-blue-900 text-white px-6 py-3 rounded-lg text-sm font-semibold hover:bg-blue-800 transition-colors flex items-center">
                <i class="fas fa-external-link-alt mr-2"></i>
                Ouvrir dans Google Maps
            </a>
            <p class="text-xs text-gray-500 mt-4">Voir GOOGLE_MAPS_SETUP.md pour la configuration</p>
        </div>
    `;
}

// Gérer l'erreur d'authentification Google Maps
window.gm_authFailure = function() {
    console.error('Google Maps authentication failed');
    showMapFallback();
};

async function loadSearchResults(query) {
    const pharmacyList = document.getElementById('pharmacyList');
    const pharmacyCount = document.getElementById('pharmacyCount');

    try {
        // First, search for the medication
        const medResponse = await fetch(`/api/medicaments/recherche?query=${encodeURIComponent(query)}`);
        const medicaments = await medResponse.json();

        if (medicaments.length === 0) {
            pharmacyList.innerHTML = `
                <div class="p-6 text-center text-gray-500 bg-gray-50 rounded-lg">
                    <i class="fas fa-search text-gray-300 text-3xl mb-3"></i>
                    <p class="font-semibold">Aucun médicament trouvé</p>
                    <p class="text-sm">Essayez avec un autre nom ou principe actif</p>
                </div>
            `;
            pharmacyCount.textContent = '0 Pharmacie';
            return;
        }

        // Get the first medication (most relevant)
        const medicament = medicaments[0];

        // Get pharmacies with this medication
        const pharmResponse = await fetch(`/api/medicaments/${medicament.id}/disponibilite?lat=${userLatitude}&lon=${userLongitude}`);
        const results = await pharmResponse.json();

        pharmacyCount.textContent = `${results.length} Pharmacie${results.length > 1 ? 's' : ''}`;
        pharmacyList.innerHTML = '';

        if (results.length === 0) {
            pharmacyList.innerHTML = `
                <div class="p-6 text-center text-gray-500 bg-gray-50 rounded-lg">
                    <i class="fas fa-exclamation-triangle text-orange-400 text-3xl mb-3"></i>
                    <p class="font-semibold">Aucune pharmacie disponible</p>
                    <p class="text-sm">Ce médicament n'est pas disponible actuellement</p>
                </div>
            `;
            return;
        }

        // Clear previous markers
        pharmacyMarkers.forEach(marker => marker.setMap(null));
        pharmacyMarkers = [];

        // Add pharmacy markers to map
        const bounds = new google.maps.LatLngBounds();
        bounds.extend(new google.maps.LatLng(userLatitude, userLongitude));

        results.forEach((result, index) => {
            const p = result.pharmacie;
            const dist = result.distance.toFixed(1);
            const disponibilite = result.disponibilite;

            // Determine stock status
            let stockStatus = 'available';
            let stockBadge = '<span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-green-100 text-green-800"><i class="fas fa-circle text-xs mr-1"></i> En Stock</span>';
            
            if (disponibilite && disponibilite.quantiteStock !== null) {
                if (disponibilite.quantiteStock <= 5) {
                    stockStatus = 'low';
                    stockBadge = '<span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-red-100 text-red-800"><i class="fas fa-exclamation-triangle text-xs mr-1"></i> Stock Faible</span>';
                }
            }

            // Determine if open 24/7
            const is24h = p.horaires && (p.horaires.toLowerCase().includes('24') || p.horaires.toLowerCase().includes('24h'));
            const hoursBadge = is24h ? '<span class="inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold bg-orange-100 text-orange-800">Ouvert 24h/24</span>' : `<span class="text-xs text-gray-600">${p.horaires || 'Horaires non disponibles'}</span>`;

            // Create marker
            const markerColor = stockStatus === 'low' ? '#dc2626' : '#16a34a';
            const marker = new google.maps.Marker({
                position: { lat: p.latitude, lng: p.longitude },
                map: map,
                title: p.nom,
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 10,
                    fillColor: markerColor,
                    fillOpacity: 1,
                    strokeColor: '#ffffff',
                    strokeWeight: 2
                }
            });

            // Add click listener to marker
            marker.addListener('click', () => {
                showPharmacyInfo(p, dist, disponibilite);
            });

            pharmacyMarkers.push(marker);
            bounds.extend(marker.getPosition());

            // Create pharmacy card
            const card = document.createElement('div');
            card.className = 'pharmacy-result-card cursor-pointer';
            card.innerHTML = `
                <div class="flex justify-between items-start mb-2">
                    <div>
                        <h3 class="font-bold text-gray-900">${p.nom}</h3>
                        <p class="text-sm text-gray-600">
                            <i class="fas fa-map-marker-alt mr-1"></i>
                            ${p.adresse} • <span class="font-semibold">${dist}km</span>
                        </p>
                    </div>
                    <span class="text-xl font-bold text-blue-900">500 FCFA</span>
                </div>
                <div class="flex items-center justify-between mb-3">
                    ${stockBadge}
                    ${hoursBadge}
                </div>
                <button class="w-full bg-blue-900 text-white py-2 rounded-lg font-semibold hover:bg-blue-800 transition-colors mb-2"
                        onclick="event.stopPropagation(); window.location.href='/medicament/${medicament.id}/pharmacie/${p.id}?lat=${userLatitude}&lon=${userLongitude}'">
                    Voir les détails
                </button>
                <button class="w-full border border-gray-300 text-gray-700 py-2 rounded-lg font-semibold hover:bg-gray-50 transition-colors"
                        onclick="event.stopPropagation(); showPharmacyOnMap(${index})">
                    Voir sur la carte
                </button>
            `;

            // Click on card to navigate to detail page
            card.addEventListener('click', (e) => {
                if (!e.target.closest('button')) {
                    window.location.href = `/medicament/${medicament.id}/pharmacie/${p.id}?lat=${userLatitude}&lon=${userLongitude}`;
                }
            });

            pharmacyList.appendChild(card);
        });

        // Fit map bounds to show all markers
        if (map && typeof map.fitBounds === 'function') {
            map.fitBounds(bounds, { padding: 100 });
        }

    } catch (error) {
        console.error('Error loading search results:', error);
        pharmacyList.innerHTML = `
            <div class="p-6 text-center text-red-500 bg-red-50 rounded-lg">
                <i class="fas fa-exclamation-circle text-3xl mb-3"></i>
                <p class="font-semibold">Erreur de chargement</p>
                <p class="text-sm">Impossible de charger les résultats: ${error.message}</p>
            </div>
        `;
    }
}

function showPharmacyOnMap(index) {
    const marker = pharmacyMarkers[index];
    if (marker) {
        map.panTo(marker.getPosition());
        map.setZoom(15);
        google.maps.event.trigger(marker, 'click');
    }
}

function showPharmacyInfo(pharmacy, distance, disponibilite) {
    let stockInfo = 'En stock';
    if (disponibilite && disponibilite.quantiteStock !== null) {
        stockInfo = `Stock: ${disponibilite.quantiteStock} unités`;
        if (disponibilite.quantiteStock <= 5) {
            stockInfo = `<span style="color: #dc2626;">${stockInfo} (Stock faible)</span>`;
        }
    }

    const infoContent = document.createElement('div');
    infoContent.innerHTML = `
        <div style="padding: 12px; font-family: Arial, sans-serif; min-width: 250px;">
            <h4 style="margin: 0 0 8px 0; font-weight: bold; color: #1f2937; font-size: 14px;">${pharmacy.nom}</h4>
            <p style="margin: 3px 0; font-size: 12px; color: #6b7280;">
                <i class="fas fa-map-marker-alt" style="color: #dc2626; margin-right: 6px;"></i>${pharmacy.adresse}
            </p>
            <p style="margin: 3px 0; font-size: 12px; color: #6b7280;">
                <i class="fas fa-phone" style="color: #0284c7; margin-right: 6px;"></i>${pharmacy.telephone}
            </p>
            <p style="margin: 3px 0; font-size: 12px; color: #6b7280;">
                <i class="fas fa-clock" style="color: #f59e0b; margin-right: 6px;"></i>${pharmacy.horaires}
            </p>
            <p style="margin: 6px 0 0 0; font-size: 12px; color: #16a34a; font-weight: bold;">
                <i class="fas fa-ruler" style="margin-right: 6px;"></i>${distance} km de distance
            </p>
            <p style="margin: 6px 0 0 0; font-size: 12px; color: #1e3a8a;">
                <i class="fas fa-box" style="margin-right: 6px;"></i>${stockInfo}
            </p>
            <a href="https://www.google.com/maps?q=${pharmacy.latitude},${pharmacy.longitude}" target="_blank" 
               style="display: inline-block; margin-top: 8px; background: #1e3a8a; color: white; padding: 6px 12px; border-radius: 6px; text-decoration: none; font-size: 12px; font-weight: 600;">
                <i class="fas fa-directions" style="margin-right: 4px;"></i>Itinéraire
            </a>
        </div>
    `;

    new google.maps.InfoWindow({
        content: infoContent
    }).open(map, pharmacyMarkers.find(m => m.getTitle() === pharmacy.nom));
}

function orderMedication(pharmacyName, medicamentName) {
    alert(`Commande de "${medicamentName}" à la pharmacie "${pharmacyName}"\n\nFonctionnalité en cours de développement.`);
}

function showDetails(id, nom, adresse, telephone, horaires, distance) {
    const modal = `
        <div class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50" onclick="this.remove()">
            <div class="bg-white rounded-2xl p-8 max-w-md w-full mx-4" onclick="event.stopPropagation()">
                <div class="flex justify-between items-start mb-4">
                    <h3 class="text-2xl font-bold text-gray-900">${nom}</h3>
                    <button onclick="this.closest('.fixed').remove()" class="text-gray-400 hover:text-gray-600">
                        <i class="fas fa-times text-xl"></i>
                    </button>
                </div>
                <div class="space-y-3 text-gray-700">
                    <p class="flex items-start">
                        <i class="fas fa-map-marker-alt text-red-500 mr-3 mt-1"></i>
                        <span>${adresse}</span>
                    </p>
                    <p class="flex items-center">
                        <i class="fas fa-phone text-blue-500 mr-3"></i>
                        <a href="tel:${telephone}" class="text-blue-600 hover:underline">${telephone}</a>
                    </p>
                    <p class="flex items-center">
                        <i class="fas fa-clock text-orange-500 mr-3"></i>
                        <span>${horaires}</span>
                    </p>
                    <p class="flex items-center">
                        <i class="fas fa-ruler text-green-500 mr-3"></i>
                        <span>${distance} km de distance</span>
                    </p>
                </div>
                <div class="mt-6 flex gap-3">
                    <a href="https://www.google.com/maps?q=${id}" target="_blank" 
                       class="flex-1 bg-blue-900 text-white py-3 rounded-lg font-semibold hover:bg-blue-800 transition-colors text-center">
                        <i class="fas fa-directions mr-2"></i>Itinéraire
                    </a>
                    <button onclick="this.closest('.fixed').remove()" 
                            class="flex-1 border border-gray-300 text-gray-700 py-3 rounded-lg font-semibold hover:bg-gray-50 transition-colors">
                        Fermer
                    </button>
                </div>
            </div>
        </div>
    `;
    document.body.insertAdjacentHTML('beforeend', modal);
}

// Filter functionality
document.addEventListener('DOMContentLoaded', () => {
    const filterButtons = document.querySelectorAll('.filter-btn');
    filterButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            btn.classList.toggle('active');
            // TODO: Implement filter logic
        });
    });
});
