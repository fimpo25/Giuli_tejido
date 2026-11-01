# Giuli Tejido

App Android para seguir proyectos de tejido.

## Qué hace

Cada proyecto tiene cuatro pestañas y un resumen arriba (tiempo, costo total y costo por hora):

- **Tiempo** — cronómetro que se pausa y se retoma sumando todo el tiempo. Sigue contando aunque se cierre la app. Guarda cada sesión, permite sumar o restar tiempo a mano y reiniciar.
- **Vueltas** — contadores con botones grandes de + y − (por defecto "Vueltas"; se pueden agregar más, por ejemplo "Puntos" o "Repeticiones").
- **Notas** — fotos (de la galería o de la cámara) y comentarios con fecha. Tocando una foto se ve en pantalla completa con zoom.
- **Costos** — artículo, precio unitario y cantidad. Muestra el subtotal de cada uno, el costo total y el costo por hora del proyecto.

En la pantalla principal, el ícono de actualización (arriba a la derecha) muestra un punto cuando hay una versión nueva; desde ahí se descarga e instala.

## Cómo se publican las versiones

Cada push a `main` dispara el workflow **Compilar APK** (`.github/workflows/build.yml`), que:

1. Compila el APK firmado.
2. Crea un Release `v1.0.N` (N = número de ejecución) con el APK adjunto y el mensaje del commit como notas.

La app consulta el último Release de este repo y, si su número es mayor que el instalado, ofrece descargarlo e instalarlo encima conservando los datos.

La primera vez hay que instalar el APK a mano desde la página de [Releases](../../releases). Android va a pedir permiso para instalar apps desde el navegador / la app de archivos, y la primera actualización desde la app va a pedir lo mismo para Giuli Tejido.

## Firma

El APK se firma siempre con `app/giuli-release.jks` para que cada versión pueda instalarse encima de la anterior. Si se reemplaza esa clave, hay que desinstalar la app (se pierden los datos) antes de instalar la nueva versión. Las contraseñas pueden sobreescribirse con los secretos `GIULI_STORE_PASSWORD` / `GIULI_KEY_PASSWORD`.

## Desarrollo

Kotlin + Jetpack Compose (Material 3), Room para los datos y Coil para las fotos. `minSdk 26` (Android 8.0).
