let userLatitude = 6.3667; // Default to Cotonou
let userLongitude = 2.4333;
let map;
let userMarker;
let pharmacyMarkers = [];
let allPharmacies = [];

document.addEventListener('DOMContentLoaded', () => {
    detectLocation();
    initMap();
    loadPharmacies();

    // Event listeners
    document.getElementById('applyFilters').addEventListener('click', applyFilters);
    document.getElementById('resetFilters')?.addEventListener('click', resetFilters);
    document.getElementById('searchPharmacy').addEventListener('keyup', () => applyFilters());
    document.getElementById('filterGarde').addEventListener('change', () => applyFilters());
    document.getElementById('filterDistance')?.addEventListener('change', () => applyFilters());
    
    // Map view button
    const mapViewBtn = document.getElementById('mapViewBtn');
    const mapSection = document.getElementById('mapSection');
    const closeMapBtn = document.getElementById('closeMapBtn');
    
    if (mapViewBtn && mapSection) {
        mapViewBtn.addEventListener('click', () => {
            mapSection.classList.remove('hidden');
            mapSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
            // Redimensionner la carte après l'affichage
            setTimeout(() => {
                google.maps.event.trigger(map, 'resize');
                if (allPharmacies.length > 0) {
                    const pharmaciesWithDistance = allPharmacies.map(p => ({
                        pharmacy: p,
                        distance: calculerDistance(userLatitude, userLongitude, p.latitude, p.longitude)
                    }));
                    updateMap(pharmaciesWithDistance);
                }
            }, 300);
        });
    }
    
    if (closeMapBtn && mapSection) {
        closeMapBtn.addEventListener('click', () => {
            mapSection.classList.add('hidden');
            window.scrollTo({ top: 0, behavior: 'smooth' });
        });
    }
});

function initMap() {
    const initialLocation = { lat: userLatitude, lng: userLongitude };

    map = new google.maps.Map(document.getElementById('mapPharmacie'), {
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

    userMarker = new google.maps.Marker({
        position: initialLocation,
        map: map,
        title: 'Votre position',
        id: 'user-marker'
    });
}

function detectLocation() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                userLatitude = position.coords.latitude;
                userLongitude = position.coords.longitude;

                if (map) {
                    const newLocation = { lat: userLatitude, lng: userLongitude };
                    map.setCenter(newLocation);
                    userMarker.setPosition(newLocation);
                }
            },
            (error) => {
                console.error("Error detecting location", error);
            }
        );
    }
}

async function loadPharmacies() {
    try {
        const response = await fetch('/api/pharmacies');
        allPharmacies = await response.json();
        displayPharmacies(allPharmacies);

        // Afficher les marqueurs sur la carte
        const pharmaciesWithDistance = allPharmacies.map(p => ({
            pharmacy: p,
            distance: calculerDistance(userLatitude, userLongitude, p.latitude, p.longitude)
        }));
        updateMap(pharmaciesWithDistance);
    } catch (error) {
        console.error("Error loading pharmacies", error);
    }
}

function applyFilters() {
    const searchTerm = document.getElementById('searchPharmacy').value.toLowerCase();
    const gardeFilter = document.getElementById('filterGarde').value;
    const distanceLimit = parseFloat(document.getElementById('filterDistance').value);

    let filtered = allPharmacies.filter(pharmacy => {
        // Search filter
        const matchesSearch = pharmacy.nom.toLowerCase().includes(searchTerm) ||
                            pharmacy.adresse.toLowerCase().includes(searchTerm);

        // Garde filter
        const matchesGarde = !gardeFilter || pharmacy.horaires.includes('24h');

        return matchesSearch && matchesGarde;
    });

    // Calculate distances and apply distance filter
    const withDistances = filtered.map(p => ({
        pharmacy: p,
        distance: calculerDistance(userLatitude, userLongitude, p.latitude, p.longitude)
    })).filter(item => item.distance <= distanceLimit);

    if (withDistances.length > 0) {
        displayPharmacies(withDistances.map(item => item.pharmacy), withDistances);
        updateMap(withDistances);
    } else {
        displayNoResults();
        clearMap();
    }
}

function resetFilters() {
    document.getElementById('searchPharmacy').value = '';
    document.getElementById('filterGarde').value = '';
    document.getElementById('filterDistance').value = '10';
    loadPharmacies();
}

function clearMap() {
    pharmacyMarkers.forEach(marker => marker.setMap(null));
    pharmacyMarkers = [];
}

function displayPharmacies(pharmacies, withDistances = null) {
    const pharmacyList = document.getElementById('pharmacyList');
    const noResults = document.getElementById('noResults');

    pharmacyList.innerHTML = '';
    noResults.classList.add('hidden');

    if (pharmacies.length === 0) {
        displayNoResults();
        return;
    }

    // Couleurs de gradient pour les cartes
    const gradients = [
        'from-teal-400 to-teal-600',
        'from-gray-200 to-gray-300',
        'from-blue-400 to-blue-600',
        'from-cyan-400 to-cyan-600',
        'from-green-400 to-green-600',
        'from-indigo-400 to-indigo-600',
        'from-purple-400 to-purple-600',
        'from-pink-400 to-pink-600'
    ];

    // Icônes pour les cartes
    const icons = [
        'fa-prescription-bottle',
        'fa-pills',
        'fa-hospital',
        'fa-capsules',
        'fa-mortar-pestle',
        'fa-clinic-medical',
        'fa-briefcase-medical',
        'fa-syringe'
    ];

    pharmacies.forEach((pharmacy, index) => {
        const distance = withDistances 
            ? withDistances.find(p => p.pharmacy.id === pharmacy.id)?.distance 
            : calculerDistance(userLatitude, userLongitude, pharmacy.latitude, pharmacy.longitude);

        const isGarde = pharmacy.horaires.includes('24h');
        const gradient = gradients[index % gradients.length];
        const icon = icons[index % icons.length];
        
        // Déterminer le statut
        let statusBadge = '';
        let statusClass = '';
        let iconOpacity = 'opacity-20';
        let iconColor = 'text-white';
        
        if (isGarde) {
            statusBadge = '<span class="w-2 h-2 bg-white rounded-full"></span>OUVERT';
            statusClass = 'bg-green-500';
        } else {
            // Simuler des statuts variés pour la démo
            const statuses = ['OUVERT', 'FERMÉ', 'GARDE UNIQ.'];
            const randomStatus = statuses[index % 3];
            
            if (randomStatus === 'OUVERT') {
                statusBadge = '<span class="w-2 h-2 bg-white rounded-full"></span>OUVERT';
                statusClass = 'bg-green-500';
            } else if (randomStatus === 'FERMÉ') {
                statusBadge = '<span class="w-2 h-2 bg-white rounded-full"></span>FERMÉ';
                statusClass = 'bg-red-500';
                iconOpacity = 'opacity-30';
                iconColor = 'text-gray-400';
            } else {
                statusBadge = '<span class="w-2 h-2 bg-white rounded-full animate-pulse"></span>GARDE UNIQ.';
                statusClass = 'bg-red-600';
            }
        }

        const card = document.createElement('div');
        card.className = 'bg-white rounded-2xl overflow-hidden shadow-sm border border-gray-200 hover:shadow-lg transition-all';
        card.innerHTML = `
            <div class="relative h-48 bg-gradient-to-br ${gradient} overflow-hidden">
                <div class="absolute inset-0 flex items-center justify-center">
                    <i class="fas ${icon} ${iconColor} text-6xl ${iconOpacity}"></i>
                </div>
                <span class="absolute top-4 right-4 ${statusClass} text-white text-xs font-bold px-3 py-1.5 rounded-full flex items-center gap-1">
                    ${statusBadge}
                </span>
            </div>
            <div class="p-5">
                <div class="flex justify-between items-start mb-3">
                    <h3 class="text-lg font-bold text-blue-900">${pharmacy.nom}</h3>
                    <button class="text-gray-400 hover:text-red-500 transition-colors">
                        <i class="far fa-heart text-xl"></i>
                    </button>
                </div>
                <div class="space-y-2 mb-4">
                    <div class="flex items-center text-gray-600 text-sm">
                        <i class="fas fa-map-marker-alt w-5 text-gray-400"></i>
                        <span>${pharmacy.adresse}</span>
                    </div>
                    <div class="flex items-center text-gray-600 text-sm">
                        <i class="fas fa-phone w-5 text-gray-400"></i>
                        <span>${pharmacy.telephone}</span>
                    </div>
                    <div class="flex items-center text-gray-600 text-sm">
                        <i class="fas fa-clock w-5 text-gray-400"></i>
                        <span>${pharmacy.horaires}</span>
                    </div>
                </div>
                <button onclick="viewPharmacyDetails(${pharmacy.id})" class="w-full py-2.5 border-2 border-blue-900 text-blue-900 rounded-xl font-semibold hover:bg-blue-900 hover:text-white transition-colors">
                    Voir les Détails
                </button>
            </div>
        `;
        pharmacyList.appendChild(card);
    });
}

function viewPharmacyDetails(pharmacyId) {
    // Trouver la pharmacie
    const pharmacy = allPharmacies.find(p => p.id === pharmacyId);
    if (pharmacy) {
        viewOnMap(pharmacy.latitude, pharmacy.longitude, pharmacy.nom);
        // Scroll vers la carte
        window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });
    }
}

function displayNoResults() {
    const pharmacyList = document.getElementById('pharmacyList');
    const noResults = document.getElementById('noResults');
    pharmacyList.innerHTML = '';
    noResults.classList.remove('hidden');
}

function updateMap(pharmaciesWithDistance) {
    // Clear previous markers
    clearMap();

    if (pharmaciesWithDistance.length === 0) {
        // Reset map to default view
        map.setCenter({ lat: userLatitude, lng: userLongitude });
        map.setZoom(13);
        return;
    }

    const bounds = new google.maps.LatLngBounds();
    bounds.extend(new google.maps.LatLng(userLatitude, userLongitude));

    pharmaciesWithDistance.forEach((item, index) => {
        const p = item.pharmacy;
        const marker = new google.maps.Marker({
            position: { lat: p.latitude, lng: p.longitude },
            map: map,
            title: p.nom,
            icon: createPharmacyMarkerIcon(index)
        });

        marker.addListener('click', () => {
            showPharmacyInfoWindow(p, item.distance);
        });

        // Ajouter un listener pour afficher les infos en survolant
        marker.addListener('mouseover', () => {
            marker.setAnimation(google.maps.Animation.BOUNCE);
        });

        marker.addListener('mouseout', () => {
            marker.setAnimation(null);
        });

        pharmacyMarkers.push(marker);
        bounds.extend(marker.getPosition());
    });

    // Fit map to bounds
    map.fitBounds(bounds, { padding: 100 });
}

function viewOnMap(lat, lng, name) {
    const location = { lat: lat, lng: lng };
    map.setCenter(location);
    map.setZoom(16);

    // Trouver et animer le marqueur correspondant
    const marker = pharmacyMarkers.find(m => m.getTitle() === name);
    if (marker) {
        marker.setAnimation(google.maps.Animation.BOUNCE);
        setTimeout(() => {
            marker.setAnimation(null);
        }, 2000);
    }
}

function showPharmacyInfoWindow(pharmacy, distance) {
    const infoContent = document.createElement('div');
    infoContent.innerHTML = `
        <div style="padding: 12px; font-family: Arial, sans-serif;">
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
                <i class="fas fa-ruler" style="margin-right: 6px;"></i>${distance.toFixed(2)} km
            </p>
        </div>
    `;

    new google.maps.InfoWindow({
        content: infoContent
    }).open(map, pharmacyMarkers.find(m => m.getTitle() === pharmacy.nom));
}

function createPharmacyMarkerIcon(index) {
    const colors = ['#16a34a', '#0284c7', '#7c3aed', '#dc2626', '#ea580c', '#d97706'];
    const color = colors[index % colors.length];

    const canvas = document.createElement('canvas');
    canvas.width = 40;
    canvas.height = 50;
    const ctx = canvas.getContext('2d');

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(20, 0);
    ctx.bezierCurveTo(28, 0, 40, 8, 40, 18);
    ctx.bezierCurveTo(40, 28, 20, 50, 20, 50);
    ctx.bezierCurveTo(20, 50, 0, 28, 0, 18);
    ctx.bezierCurveTo(0, 8, 12, 0, 20, 0);
    ctx.fill();

    ctx.fillStyle = 'white';
    ctx.beginPath();
    ctx.arc(20, 18, 10, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = color;
    ctx.font = 'bold 16px Arial';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText(String(index + 1), 20, 18);

    return {
        url: canvas.toDataURL(),
        scaledSize: new google.maps.Size(40, 50),
        anchor: new google.maps.Point(20, 50)
    };
}

function calculerDistance(lat1, lon1, lat2, lon2) {
    const R = 6371;
    const latDistance = Math.toRadians(lat2 - lat1);
    const lonDistance = Math.toRadians(lon2 - lon1);
    const a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
}

Math.toRadians = function(degrees) {
    return degrees * Math.PI / 180;
};






