# Antrenamente Aikido

Aplicație Android care ține evidența prezenței la antrenamentele de aikido, în loc de un tabel Excel completat manual. În fiecare seară de antrenament, telefonul te întreabă dacă ai fost, iar răspunsul rămâne salvat într-un calendar cu statistici.

## Ce face

- **Te întreabă, nu trebuie să-ți amintești tu.** După ora antrenamentului, primești o notificare cu două butoane, **Am fost** și **N-am fost**, la care răspunzi direct, fără să deschizi aplicația.
- **Nu uită nimic.** Dacă o zi rămâne fără răspuns, aplicația o ține minte și te întreabă din nou a doua zi, până răspunzi.
- **Ține cont de stagii și examene**, care pot cădea în orice zi, nu doar în zilele obișnuite de antrenament.
- **Arată un calendar colorat**, cu verde pentru prezență, roșu pentru absență, gri pentru „nu s-a ținut” și auriu pentru examene.
- **Calculează statistici** pe lună și pe an: câte antrenamente ai făcut din câte au fost programate, trecute și viitoare.
- **Face singură o copie de siguranță**, o dată pe săptămână, într-un fișier pe care-l alegi tu, de exemplu pe Google Drive.

## Cum funcționează, pas cu pas

### Programul de antrenament

Aplicația presupune antrenamente **luni, miercuri și vineri**. Zilele astea sunt fixate în cod, nu se schimbă din aplicație; dacă dojo-ul tău are alt program, codul trebuie modificat.

### Întrebarea zilnică

În fiecare zi de antrenament, la o oră pe care o alegi din Setări (implicit 21:00), aplicația verifică dacă mai există zile fără răspuns:

- **Ziua de azi** intră în calcul abia după ora stabilită, ca să nu fii întrebat înainte ca antrenamentul să se fi ținut.
- **Zilele trecute**, dacă au rămas fără răspuns, apar imediat, de la cea mai veche la cea mai nouă.
- Pentru fiecare zi deschisă primești o **notificare separată**, cu butoanele **Am fost** / **N-am fost**. Dacă răspunzi uneia, imediat apare următoarea, dacă mai e nerezolvată.
- Poți răspunde și mai târziu, direct din aplicație: zilele rămase deschise apar pe ecranul principal.

Android poate întârzia notificarea cu până la o oră, din cauza optimizărilor de baterie ale telefonului.

### Stagii și examene

Din ecranul principal sau din calendar, poți adăuga un stagiu sau un examen pe o zi sau pe un interval de zile, cu un nume ales de tine (de exemplu „Stagiu Perpelici”). Aceste zile intră în aceeași întrebare zilnică, chiar dacă nu sunt zile obișnuite de antrenament.

### Răspunsuri posibile

- **Am fost** — prezență, intră în statistici.
- **N-am fost** — absență, intră în statistici.
- **Nu s-a ținut** — pentru zile în care antrenamentul a fost anulat sau nu te privea (de exemplu un examen al altcuiva). Nu intră în statistici.

Orice răspuns se poate schimba oricând, din calendar.

### Calendar și statistici

Calendarul arată fiecare zi colorată după răspuns. Pe ecranul principal și în calendar apar statistici de forma „x / total”:

- **Totalul** cuprinde toate zilele de antrenament (luni, miercuri, vineri) din perioadă, plus stagiile și examenele, **inclusiv zilele viitoare, care nu au avut loc încă**. Zilele „nu s-a ținut” nu intră în total.
- **Procentul** se calculează doar din zilele la care ai răspuns deja, ca să rămână corect și la mijlocul lunii, fără să scadă artificial din cauza zilelor viitoare.

### Copii de siguranță

Datele stau doar pe telefon, într-o bază de date locală; aplicația nu are server și nu trimite nimic nicăieri de la sine.

- **Copie manuală**, din Setări: salvează un fișier oricând vrei, de exemplu înainte să schimbi telefonul.
- **Copie automată**, tot din Setări: alegi o singură dată un fișier, de obicei pe Google Drive, iar aplicația îl suprascrie o dată pe săptămână, atât timp cât telefonul are internet.
- **Restaurare**: înlocuiește toate datele din aplicație cu cele din fișierul ales. Un fișier care nu e o copie validă e refuzat, fără să schimbe nimic.

O copie nu salvează ora întrebării, doar datele de prezență.

## Instalare

Aplicația nu e pe Google Play. Se instalează din fișierul APK, descărcat din secțiunea **Releases** a acestui repo:

1. Descarcă fișierul `.apk` de la ultima versiune publicată.
2. Deschide-l pe telefon. Dacă apare un avertisment despre „surse necunoscute”, permite instalarea.
3. La prima pornire, permite notificările, altfel întrebarea zilnică nu poate apărea.
4. Recomandat: scoate aplicația de la optimizarea bateriei telefonului, ca notificările și copia automată să nu fie blocate.

Necesită Android 8.0 sau mai nou.

## Pentru cine instalează aplicația asta acum

Istoricul de prezență inclus în aplicație (`app/src/main/assets/history.json`) aparține autorului și se încarcă automat la prima instalare pe orice telefon. **Oricine instalează acest APK pornește cu istoricul autorului din 2026, nu cu unul gol.** Dacă vrei să distribui aplicația mai departe altor colegi, asta trebuie rezolvat întâi (de exemplu înlocuind fișierul cu unul gol), ca fiecare să pornească fără datele altcuiva.

## Tehnologii folosite

Kotlin și Jetpack Compose (Material 3), bază de date SQLite locală, notificări native Android și copii de siguranță prin selectorul de fișiere Android (fără cont Google Cloud sau server propriu).
