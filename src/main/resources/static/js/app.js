let userLatitude = 6.3667; // Default to Cotonou
let userLongitude = 2.4333;

document.addEventListener('DOMContentLoaded', () => {
    detectLocation();

    const searchBtn = document.getElementById('searchBtn');
    const searchInput = document.getElementById('searchInput');
    const quickResultsDropdown = document.getElementById('quickResultsDropdown');

    if (searchBtn && searchInput) {
        searchBtn.addEventListener('click', () => {
            performSearch(searchInput.value);
        });

        searchInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') {
                performSearch(searchInput.value);
            }
        });

        // Real-time search with dropdown
        searchInput.addEventListener('input', (e) => {
            const query = e.target.value.trim();

            if (query.length < 2) {
                if (quickResultsDropdown) {
                    quickResultsDropdown.classList.add('hidden');
                }
                return;
            }

            performQuickSearch(query);
        });

        // Hide dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('.search-container') && quickResultsDropdown) {
                quickResultsDropdown.classList.add('hidden');
            }
        });
    }
});

function detectLocation() {
    const locationText = document.getElementById('locationText');
    if (!locationText) return;
    
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            (position) => {
                userLatitude = position.coords.latitude;
                userLongitude = position.coords.longitude;
                locationText.innerText = `Position détectée`;
            },
            (error) => {
                console.error("Error detecting location", error);
                locationText.innerText = "Utiliser ma position actuelle";
            }
        );
    } else {
        locationText.innerText = "Utiliser ma position actuelle";
    }
}

async function performQuickSearch(query) {
    const quickResultsDropdown = document.getElementById('quickResultsDropdown');
    const quickResultsList = document.getElementById('quickResultsList');

    if (!quickResultsDropdown || !quickResultsList) return;

    try {
        const response = await fetch(`/api/medicaments/recherche?query=${encodeURIComponent(query)}`);
        const medicaments = await response.json();

        quickResultsList.innerHTML = '';

        if (medicaments.length > 0) {
            medicaments.slice(0, 5).forEach(med => {
                const resultItem = document.createElement('div');
                resultItem.className = 'p-4 cursor-pointer hover:bg-gray-50 transition-colors';
                resultItem.innerHTML = `
                    <div class="flex items-center justify-between">
                        <div class="flex-1">
                            <p class="font-semibold text-gray-800">${med.nom}</p>
                            <p class="text-xs text-gray-500">${med.principeActif} • ${med.dosage}</p>
                        </div>
                        <i class="fas fa-arrow-right text-blue-900"></i>
                    </div>
                `;
                resultItem.addEventListener('click', () => {
                    performSearch(med.nom);
                });
                quickResultsList.appendChild(resultItem);
            });

            quickResultsDropdown.classList.remove('hidden');
        } else {
            quickResultsList.innerHTML = `
                <div class="p-4 text-center text-gray-500">
                    <i class="fas fa-search text-gray-300 text-2xl mb-2"></i>
                    <p>Aucun médicament trouvé</p>
                </div>
            `;
            quickResultsDropdown.classList.remove('hidden');
        }
    } catch (error) {
        console.error("Quick search error", error);
    }
}

async function performSearch(query) {
    if (!query || query.trim().length < 2) return;

    // Redirect to search results page with query parameter
    window.location.href = `/search-results?query=${encodeURIComponent(query)}&lat=${userLatitude}&lon=${userLongitude}`;
}

