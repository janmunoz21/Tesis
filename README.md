<div align="center">

# Plataforma Móvil para la Creación Colaborativa de Álbumes Inteligentes mediante Procesamiento Distribuido

### Procesamiento Distribuido de Imágenes en Redes P2P Móviles con Modelo de Recompensas

[![Plataforma](https://img.shields.io/badge/Plataforma-Android%2010+-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Inferencia](https://img.shields.io/badge/Inferencia-TensorFlow%20Lite-FF6F00?style=for-the-badge&logo=tensorflow&logoColor=white)](https://www.tensorflow.org/lite)
[![Modelo](https://img.shields.io/badge/Modelo-MobileNetV3-yellow?style=for-the-badge)]()
[![Comunicación](https://img.shields.io/badge/Comunicaci%C3%B3n-gRPC%20%7C%20mDNS-244C5A?style=for-the-badge)](https://grpc.io)
[![Persistencia](https://img.shields.io/badge/Persistencia-Room%20%7C%20PostgreSQL-336791?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org)
[![Versión](https://img.shields.io/badge/Versi%C3%B3n-1.0%20Final-blue?style=for-the-badge)]()

</div>

---

## 1. Ficha Técnica del Proyecto

| Parámetro | Detalle Institucional y de Proyecto |
| :--- | :--- |
| **Título formal** | Procesamiento Distribuido de Imágenes en Redes P2P Móviles con Modelo de Recompensas |
| **Institución** | Pontificia Universidad Javeriana (Sede Bogotá) — Facultad de Ingeniería, Departamento de Ingeniería de Sistemas |
| **Director de investigación** | Ing. Alejandro Castro |
| **Autores** | Alejandro Castelblanco Arias, Andrés Camilo Rincón Alfonso, Diego Alejandro Viera Herrera y Jan Marco Muñoz Pineda |
| **Fecha y versión** | 30 de septiembre de 2026 (Versión 0.0) |
| **Dominio tecnológico** | Cómputo fragmentado (*Split Computing*), visión artificial en el dispositivo (*Edge Computing*) y redes inalámbricas locales descentralizadas |

---

## 2. Visión General del Sistema

El proyecto consiste en una aplicación móvil nativa para el sistema operativo Android orientada a permitir que un grupo de personas reúna, procese y organice colaborativamente las fotografías tomadas en un evento compartido (viajes, reuniones familiares, excursiones o actividades universitarias).

En vez de subir colecciones pesadas a servidores externos en la nube —lo que acarrea un alto consumo de datos móviles y riesgos para la privacidad—, el sistema aprovecha las capacidades de cómputo de los propios celulares presentes en el lugar. Al conectarse a una red inalámbrica local (Wi-Fi), los dispositivos conforman una red entre pares (*Peer-to-Peer* o P2P). El teléfono anfitrión divide el lote de imágenes en bloques y los delega a los teléfonos colaboradores disponibles. Cada terminal ejecuta un modelo liviano de aprendizaje profundo para clasificar las imágenes por escenas y temáticas, permitiendo compilar automáticamente un álbum organizado y depurado sin enviar las fotos originales a internet.

---

## 3. Planteamiento del Problema

Tras experiencias grupales suelen acumularse cientos o miles de tomas fotográficas dispersas entre los asistentes. Esto genera problemas recurrentes:

* **Fragmentación del contenido:** Las capturas quedan guardadas en múltiples celulares sin un repositorio unificado.
* **Presencia de archivos repetidos o defectuosos:** Abundan las imágenes movidas, oscuras, desenfocadas o duplicadas.
* **Consumo de datos y pérdida de privacidad:** El uso de almacenamiento en la nube comercial expone las imágenes y consume planes móviles.
* **Postergación de la organización:** Clasificar manualmente cientos de archivos demanda un tiempo considerable, por lo que los álbumes rara vez se completan.

---

## 4. Solución Propuesta y Flujo Operativo

La aplicación automatiza la compilación del material fotográfico mediante un flujo continuo en red local:

```
[1. Creación de Sesión] ────► [2. Aporte de Fotos] ────► [3. Detección mDNS]
                                                                │
[6. Entrega de Álbum]   ◄──── [5. Inferencia Local] ◄──── [4. Segmentación P2P]
```

1. **Creación de la sesión:** Un participante crea el álbum y comparte un código de acceso o enlace con el grupo.
2. **Aporte de archivos:** Cada integrante elige desde su galería las fotografías que desea aportar.
3. **Detección automática de terminales:** Los teléfonos enlazados a la misma red inalámbrica se descubren e intercambian métricas de disponibilidad.
4. **Segmentación y distribución:** El nodo anfitrión fracciona las imágenes en bloques equilibrados según la capacidad de cada celular.
5. **Inferencia local en los dispositivos:** Cada teléfono procesa su bloque con una red neuronal artificial liviana, generando categorías de escenas y niveles de certidumbre.
6. **Consolidación y entrega:** El anfitrión recopila los resultados, descarta tomas defectuosas, estructura subálbumes temáticos (comida, playa, naturaleza, reuniones) y entrega el álbum listo para consulta a todos los integrantes.

---

## 5. Roles y Dinámica de los Nodos

El sistema opera bajo dos roles de trabajo complementarios:

| Rol | Naturaleza | Funciones y Responsabilidades Principales |
| :--- | :--- | :--- |
| **Nodo Principal** | *Consumidor / Orquestador* | Recibe fotos aportadas, preprocesa archivos a 224 × 224 píxeles, divide los bloques de trabajo, monitorea el avance del grupo de dispositivos y consolida el álbum definitivo. |
| **Nodo Colaborador** | *Proveedor / Trabajador* | Mantiene un servicio en segundo plano, recibe bloques asignados, ejecuta inferencias neuronales sobre las fotos y envía los vectores de clasificación. |

> 🛡️ **Mecanismo de Respaldo Local ante Fallos**
> * El nodo principal evalúa continuamente la disponibilidad de cada terminal colaborador.
> * Si un dispositivo pierde la conexión Wi-Fi, se apaga o sobrepasa un tiempo límite de espera de **800 ms**, el sistema revoca la asignación y recupera las fotos pendientes.
> * Las imágenes no completadas se reasignan a otro colaborador activo o se procesan localmente en el procesador del nodo principal en un tiempo inferior a **3 s**, garantizando la continuidad de la tarea.

---

## 6. Arquitectura y Despliegue del Sistema

El sistema implementa una arquitectura híbrida: el procesamiento masivo y la transmisión de imágenes ocurren dentro de la red inalámbrica local (WLAN), mientras que la autenticación de usuarios y el control de sesiones se gestionan mediante servicios en la nube a través de una pasarela segura.

### Diagrama de Arquitectura del Sistema

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/3faa1acc-6401-42a4-a5c4-db43ad9891c1">
    <img alt="Diagrama de Arquitectura del Sistema" src="https://github.com/user-attachments/assets/3faa1acc-6401-42a4-a5c4-db43ad9891c1" width="95%">
  </picture>
  <br>
  <em>Figura 1: Arquitectura lógica y distribución de capas del sistema.</em>
</p>

### Componentes de Despliegue Físico y Comunicación

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/e82b854c-c8b5-4fa5-9caf-d78c2e00058a">
    <img alt="Diagrama de Despliegue Físico" src="https://github.com/user-attachments/assets/e82b854c-c8b5-4fa5-9caf-d78c2e00058a" width="95%">
  </picture>
  <br>
  <em>Figura 2: Nodos de ejecución física y protocolos de interconexión.</em>
</p>

* **Motor de Inferencia:** Emplea **MobileNetV3** optimizado para terminales móviles mediante **TensorFlow Lite**, aprovechando aceleración por procesadores gráficos (GPU) y unidades de procesamiento neuronal (NPU).
* **Preprocesamiento:** Cada captura se ajusta obligatoriamente a **224 × 224 píxeles** y se normaliza para coincidir con la matriz esperada por el modelo.
* **Descubrimiento de Dispositivos:** Se utiliza **mDNS / DNS-SD** (RFC 6762) en el puerto UDP 5353, permitiendo que los celulares se ubiquen en la red Wi-Fi sin configurar direcciones IP de forma manual.
* **Transferencia entre Terminales:** La entrega de bloques y el retorno de resultados se realiza mediante **gRPC sobre HTTP/2**, serializando los mensajes con **Protocol Buffers (Proto3)** y protegiendo el canal local con **TLS v1.3**.
* **Persistencia Local Desconectada:** Cada celular incorpora una base de datos local mediante **Room (SQLite)** de Android Jetpack, permitiendo explorar los álbumes sin depender de internet.
* **Servidor Central:** Interfaz de servicios desarrollada para autenticación e identidades con fichas de seguridad (JWT) sobre HTTPS y persistencia en **PostgreSQL** bajo transacciones atómicas (ACID).

---

## 7. Módulos y Diagramas de Secuencia por Caso de Uso

### Módulo B — Gestión de Álbumes
Abarca la creación, administración de participantes, configuración de colecciones y visualización general.

#### CU-B-01 — Crear álbum
Describe el flujo en el que un usuario registrado define el nombre y descripción del álbum, inicializando la sesión colaborativa en el almacenamiento local y preparando la sala de invitación.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/7f3a1103-7cf8-4eaa-9cd5-02c2b471ae89">
    <img alt="CU-B-01 — Crear álbum" src="https://github.com/user-attachments/assets/7f3a1103-7cf8-4eaa-9cd5-02c2b471ae89" width="90%">
  </picture>
  <br>
  <em>Figura 3: Diagrama de secuencia — Creación de álbum y apertura de sesión.</em>
</p>

---

### Módulo C — Red del Grupo y Tolerancia a Fallos
Monitorea la visibilidad de los celulares participantes en la red local y gestiona contingencias operativas.

#### CU-C-01 — Descubrir y visualizar nodos disponibles del álbum
El participante consulta la sección de red, donde el sistema sondea la red Wi-Fi mediante mDNS y lista los terminales activos junto con su modelo, potencia de señal y estado de disponibilidad.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/806710c5-a769-4383-9758-d88348d56ca8">
    <img alt="CU-C-01 — Descubrir y visualizar nodos disponibles del álbum" src="https://github.com/user-attachments/assets/806710c5-a769-4383-9758-d88348d56ca8" width="90%">
  </picture>
  <br>
  <em>Figura 4: Diagrama de secuencia — Descubrimiento y sondeo de nodos locales.</em>
</p>

#### CU-C-03 — Reasignar bloque cuando un nodo falla
Si un nodo colaborador deja de emitir respuesta dentro del tiempo límite establecido, el orquestador declara el bloque incompleto y redirige las fotos pendientes a otro colaborador o las procesa localmente.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/6e9bf2e1-3430-46d1-b48c-4eb10867a41b">
    <img alt="CU-C-03 — Reasignar bloque cuando un nodo falla" src="https://github.com/user-attachments/assets/6e9bf2e1-3430-46d1-b48c-4eb10867a41b" width="90%">
  </picture>
  <br>
  <em>Figura 5: Diagrama de secuencia — Manejo de caídas y reasignación de bloques de fotos.</em>
</p>

---

### Módulo D — Subida y Procesamiento Distribuido
Núcleo del sistema encargado de cargar el material visual, segmentar las tareas, ejecutar la inferencia neuronal y unificar los resultados.

#### CU-D-01 — Subir fotos a un álbum
El integrante elige una o varias fotografías de su galería, valida que cumplan con los límites de tamaño permitidos y las encola para el proceso de análisis.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/84358439-581b-4f4e-b62b-cb45850578f0">
    <img alt="CU-D-01 — Subir fotos a un álbum" src="https://github.com/user-attachments/assets/84358439-581b-4f4e-b62b-cb45850578f0" width="90%">
  </picture>
  <br>
  <em>Figura 6: Diagrama de secuencia — Carga y preparación inicial de imágenes.</em>
</p>

#### CU-D-02 — Distribuir bloques de fotos entre los nodos del grupo
El nodo anfitrión determina el número de colaboradores activos, fragmenta las imágenes en paquetes proporcionados y envía las propuestas de asignación a cada teléfono mediante llamadas a procedimiento remoto.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/59e01d23-0709-4e21-9cc5-8d78c9a8dc1c">
    <img alt="CU-D-02 — Distribuir bloques de fotos entre los nodos del grupo" src="https://github.com/user-attachments/assets/59e01d23-0709-4e21-9cc5-8d78c9a8dc1c" width="90%">
  </picture>
  <br>
  <em>Figura 7: Diagrama de secuencia — Distribución y partición equilibrada de bloques de trabajo.</em>
</p>

#### CU-D-04 — Procesar bloque de fotos localmente
El nodo colaborador carga el modelo MobileNetV3 en memoria, clasifica cada fotografía del bloque recibido, calcula la confianza estadística y retorna las etiquetas generadas al nodo anfitrión.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/8be4ad5e-e6ea-48a8-9060-fc95ee84c419">
    <img alt="CU-D-04 — Procesar bloque de fotos localmente" src="https://github.com/user-attachments/assets/8be4ad5e-e6ea-48a8-9060-fc95ee84c419" width="90%">
  </picture>
  <br>
  <em>Figura 8: Diagrama de secuencia — Ejecución de inferencia neuronal en el nodo colaborador.</em>
</p>

#### CU-D-06 — Consolidar resultados y notificar álbum listo
El orquestador unifica las clasificaciones recibidas, agrupa las fotos en subálbumes temáticos, genera el resumen de la sesión y emite la notificación de finalización a todos los participantes.

<p align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="https://github.com/user-attachments/assets/575a1e9c-70c9-4983-9ec2-304af15ff0c8">
    <img alt="CU-D-06 — Consolidar resultados y notificar álbum listo" src="https://github.com/user-attachments/assets/575a1e9c-70c9-4983-9ec2-304af15ff0c8" width="90%">
  </picture>
  <br>
  <em>Figura 9: Diagrama de secuencia — Agrupación temática final y entrega a los participantes.</em>
</p>

---

### Módulos Complementarios

* **Módulo A — Cuenta y Perfil de Usuario:** Registro de cuentas, inicio de sesión seguro, control de privacidad y activación manual o automática del modo colaborador en segundo plano.
* **Módulo E — Categorización y Visualización:** Vista interactiva del álbum terminado, segmentado en subálbumes temáticos (playa, comida, reuniones, atardeceres) con la opción de reclasificar imágenes manualmente si la confianza estadística del modelo fue baja.
* **Módulo F — Descarga y Difusión:** Opciones para exportar el álbum completo a la memoria del teléfono, descargar únicamente una categoría específica o compartir imágenes individuales en resolución nativa.

---

## 8. Atributos de Calidad y Restricciones Técnicas

| Parámetro Operativo | Criterio de Rendimiento | Entorno y Condiciones |
| :--- | :--- | :--- |
| **Inferencia extremo a extremo** | $\le$ 1.500 ms por bloque | Red local con ancho de banda $\ge$ 20 Mbps y latencia $\le$ 15 ms |
| **Tiempo de serialización** | $\le$ 80 ms de procesador | Serialización de mensajes mediante Protocol Buffers |
| **Descubrimiento de red** | $\le$ 2.500 ms de resolución | Escaneo concurrente de hasta 15 terminales vía mDNS |
| **Sistema operativo móvil** | Android 10 (API 29)+ | Compatibilidad base a partir de Android 8.0 (API 26) |
| **Memoria de trabajo (RAM)** | Mínimo 3 GB libres | Prevención de detención de servicios en segundo plano |
| **Almacenamiento y captura** | $\ge$ 100 MB libres / Cámara $\ge$ 8 MP | Espacio para persistencia SQLite y modelo cuantizado |

---

## 9. Beneficios e Impacto del Proyecto

1. **Privacidad integral:** Los archivos multimedia de los usuarios se transmiten únicamente dentro del perímetro físico de la red inalámbrica local; las imágenes personales jamás se cargan ni almacenan en servidores remotos en la nube.
2. **Independencia de la conexión a internet:** El flujo intensivo de cómputo, clasificación y distribución opera de manera autónoma sobre la red local Wi-Fi, eliminando costos por uso de paquetes de datos móviles.
3. **Eficiencia y reducción de tiempos:** Al segmentar y procesar en paralelo las fotografías aprovechando la potencia combinada de múltiples dispositivos, se reducen notablemente los tiempos de espera en comparación con la ejecución en un solo terminal.
4. **Entrega automatizada del producto:** Los participantes del evento obtienen de forma inmediata un álbum curado, ordenado temáticamente y libre de capturas defectuosas o redundantes.

---

## 10. Flujo y Demostración de la Aplicación

Registro audiovisual del funcionamiento real del sistema, ilustrando la conexión entre pares, la distribución de tareas y el resultado final:

https://github.com/user-attachments/assets/3de3035c-bbd0-4337-81e3-94d302a39d53
