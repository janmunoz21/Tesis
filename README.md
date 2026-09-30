# Wiki del Proyecto: Plataforma Móvil para la Creación Colaborativa de Álbumes Inteligentes mediante Procesamiento Distribuido

---

## 1. Ficha Técnica del Proyecto

* **Título formal:** Procesamiento Distribuido de Imágenes en Redes P2P Móviles con Modelo de Recompensas[cite: 1].
* **Institución:** Pontificia Universidad Javeriana (Sede Bogotá) — Facultad de Ingeniería, Departamento de Ingeniería de Sistemas[cite: 1, 3].
* **Director de investigación:** Ing. Alejandro Castro[cite: 3].
* **Autores:** Alejandro Castelblanco Arias, Andrés Camilo Rincón Alfonso, Diego Alejandro Viera Herrera y Jan Marco Muñoz Pineda[cite: 2].
* **Fecha y versión:** 25 de mayo de 2026 (Versión Final 1.0)[cite: 1, 2].
* **Dominio tecnológico:** Cómputo fragmentado (*Split Computing*), visión por computador en el borde (*Edge Computing*) y redes inalámbricas locales descentralizadas[cite: 3, 4].

---

## 2. Visión General del Sistema

El proyecto consiste en una aplicación móvil nativa para el sistema operativo Android orientada a permitir que un grupo de personas reúna, procese y organice colaborativamente las fotografías tomadas en un evento compartido (viajes, reuniones familiares, excursiones o actividades universitarias)[cite: 3, 5].

En vez de subir colecciones pesadas a servidores externos en la nube —lo que acarrea un alto consumo de datos móviles y riesgos para la privacidad—, el sistema aprovecha las capacidades de cómputo de los propios celulares presentes en el lugar[cite: 3, 5]. Al conectarse a una red inalámbrica local (Wi-Fi), los dispositivos conforman una red entre pares (*Peer-to-Peer* o P2P)[cite: 3, 6, 22]. El teléfono anfitrión divide el lote de imágenes en bloques y los delega a los teléfonos colaboradores disponibles[cite: 3, 9, 23]. Cada terminal ejecuta un modelo liviano de aprendizaje profundo para clasificar las imágenes por escenas y temáticas, permitiendo compilar automáticamente un álbum organizado y depurado sin enviar las fotos originales a internet[cite: 3, 6, 9].

---

## 3. Planteamiento del Problema

Tras experiencias grupales suelen acumularse cientos o miles de tomas fotográficas dispersas entre los asistentes[cite: 3, 6]. Esto genera problemas recurrentes:

* **Fragmentación del contenido:** Las capturas quedan guardadas en múltiples celulares sin un repositorio unificado[cite: 3].
* **Presencia de archivos repetidos o defectuosos:** Abundan las imágenes movidas, oscuras, desenfocadas o duplicadas[cite: 3].
* **Consumo de datos y pérdida de privacidad:** El uso de almacenamiento en la nube comercial expone las imágenes y consume planes móviles[cite: 3].
* **Postergación de la organización:** Clasificar manualmente cientos de archivos demanda un tiempo considerable, por lo que los álbumes rara vez se completan[cite: 3].

---

## 4. Solución Propuesta y Flujo Operativo

La aplicación automatiza la compilación del material fotográfico mediante un flujo distribuido en red local[cite: 3, 6]:

1. **Creación de la sesión:** Un participante crea el álbum y comparte un código de acceso o enlace con el grupo[cite: 3, 10].
2. **Aporte de archivos:** Cada integrante elige desde su galería las fotografías que desea aportar[cite: 6, 9, 23].
3. **Detección automática de terminales:** Los teléfonos enlazados a la misma red inalámbrica se descubren e intercambian métricas de disponibilidad[cite: 6, 8, 25].
4. **Segmentación y distribución:** El nodo anfitrión fracciona las imágenes en bloques equilibrados según la capacidad de cada celular[cite: 9, 13, 23].
5. **Inferencia local en los dispositivos:** Cada teléfono procesa su bloque con una red neuronal artificial liviana, generando categorías de escenas y niveles de certidumbre[cite: 6, 7, 23].
6. **Consolidación y entrega:** El anfitrión recopila los resultados, descarta tomas defectuosas, estructura subálbumes temáticos (comida, playa, naturaleza, reuniones) y entrega el álbum listo para consulta a todos los integrantes[cite: 9, 15, 23].

---

## 5. Roles y Dinámica de los Nodos

El sistema opera bajo dos roles de trabajo complementarios[cite: 3, 6, 11]:

| Rol | Descripción | Funciones Principales |
| :--- | :--- | :--- |
| **Nodo Principal** *(Consumidor / Orquestador)*[cite: 3, 11] | Terminal que coordina la sesión del álbum y la partición del trabajo[cite: 3, 11]. | Recibe fotos aportadas, preprocesa archivos a 224x224 píxeles, divide los bloques de trabajo, monitorea el avance del clúster y consolida el álbum definitivo[cite: 6, 9, 13, 23]. |
| **Nodo Colaborador** *(Proveedor / Trabajador)*[cite: 3, 11] | Terminal que aporta su capacidad de cálculo cuando no está en uso intensivo[cite: 3, 11]. | Mantiene un servicio en segundo plano, recibe bloques asignados, ejecuta inferencias neuronales sobre las fotos y envía los vectores de clasificación[cite: 6, 9, 12, 23]. |

### Mecanismo de Respaldo Local ante Fallos
* El nodo principal evalúa continuamente la disponibilidad de cada terminal colaborador[cite: 9, 26].
* Si un dispositivo pierde la conexión Wi-Fi, se apaga o sobrepasa un tiempo límite de espera de 800 milisegundos, el sistema revoca la asignación y recupera las fotos pendientes[cite: 26].
* Las imágenes no completadas se reasignan a otro colaborador activo o se procesan localmente en el procesador del nodo principal en un tiempo inferior a 3 segundos, garantizando la continuidad de la tarea[cite: 9, 23, 26].

---

## 6. Arquitectura y Despliegue del Sistema

El sistema implementa una arquitectura híbrida: el procesamiento masivo y la transmisión de imágenes ocurren dentro de la red inalámbrica local (WLAN), mientras que la autenticación de usuarios y el control de sesiones se gestionan mediante servicios en la nube a través de una pasarela segura[cite: 25, 26].

![Diagrama Arquitectura](https://github.com/user-attachments/assets/3faa1acc-6401-42a4-a5c4-db43ad9891c1)

### Componentes de Despliegue Físico y Comunicación

![Diagrama Despliegue V3](https://github.com/user-attachments/assets/e82b854c-c8b5-4fa5-9caf-d78c2e00058a)

* **Motor de Inferencia:** Emplea **MobileNetV3** optimizado para terminales móviles mediante **TensorFlow Lite**, aprovechando aceleración por procesadores gráficos (GPU) y unidades de procesamiento neuronal (NPU)[cite: 4, 7, 22, 25].
* **Preprocesamiento:** Cada captura se ajusta obligatoriamente a $224\times224$ píxeles y se normaliza para coincidir con la matriz esperada por el modelo[cite: 9, 13, 25].
* **Descubrimiento de Dispositivos:** Se utiliza **mDNS / DNS-SD** (RFC 6762) en el puerto UDP 5353, permitiendo que los celulares se ubiquen en la red Wi-Fi sin configurar direcciones IP de forma manual[cite: 4, 8, 20, 22, 25].
* **Transferencia entre Terminales:** La entrega de bloques y el retorno de resultados se realiza mediante **gRPC sobre HTTP/2**, serializando los mensajes con **Protocol Buffers (Proto3)** y protegiendo el canal local con **TLS v1.3**[cite: 6, 7, 8, 22, 26].
* **Persistencia Local Desconectada:** Cada celular incorpora una base de datos local mediante **Room (SQLite)** de Android Jetpack, permitiendo explorar los álbumes sin depender de internet[cite: 8, 22].
* **Servidor Central:** API desarrollada para autenticación e identidades con fichas de seguridad (JWT) sobre HTTPS y persistencia en **PostgreSQL** bajo transacciones atómicas (ACID)[cite: 22, 26, 27].

---

## 7. Módulos y Diagramas de Secuencia por Caso de Uso

### Módulo B — Gestión de Álbumes
Abarca la creación, administración de participantes, configuración de colecciones y visualización general[cite: 16, 23].

#### CU-B-01 — Crear álbum
Describe el flujo en el que un usuario registrado define el nombre y descripción del álbum, inicializando la sesión colaborativa en el almacenamiento local y preparando la sala de invitación[cite: 16].

![CU-B-01 — Crear álbum](https://github.com/user-attachments/assets/7f3a1103-7cf8-4eaa-9cd5-02c2b471ae89)

---

### Módulo C — Red del Grupo
Monitorea la visibilidad de los celulares participantes en la red local y gestiona contingencias operativas[cite: 17, 20, 23].

#### CU-C-01 — Descubrir y visualizar nodos disponibles del álbum
El participante consulta la sección de red, donde el sistema sondea la red Wi-Fi mediante mDNS y lista los terminales activos junto con su modelo, potencia de señal y estado de disponibilidad[cite: 17].

![CU-C-01 — Descubrir y visualizar nodos disponibles del álbum](https://github.com/user-attachments/assets/806710c5-a769-4383-9758-d88348d56ca8)

#### CU-C-03 — Reasignar bloque cuando un nodo falla
Si un nodo colaborador deja de emitir respuesta dentro del tiempo límite establecido, el orquestador declara el bloque incompleto y redirige las fotos pendientes a otro colaborador o las procesa localmente[cite: 20, 26].

![CU-C-03 — Reasignar bloque cuando un nodo falla](https://github.com/user-attachments/assets/6e9bf2e1-3430-46d1-b48c-4eb10867a41b)

---

### Módulo D — Subida y Procesamiento Distribuido
Núcleo del sistema encargado de cargar el material visual, segmentar las tareas, ejecutar la inferencia neuronal y unificar los resultados[cite: 18, 19, 23].

#### CU-D-01 — Subir fotos a un álbum
El integrante elige una o varias fotografías de su galería, valida que cumplan con los límites de tamaño permitidos y las encola para el proceso de análisis[cite: 18].

![CU-D-01 — Subir fotos a un álbum](https://github.com/user-attachments/assets/84358439-581b-4f4e-b62b-cb45850578f0)

#### CU-D-02 — Distribuir bloques de fotos entre los nodos del grupo
El nodo anfitrión determina el número de colaboradores activos, fragmenta las imágenes en paquetes proporcionados y envía las propuestas de asignación a cada teléfono mediante llamadas a procedimiento remoto[cite: 18].

![CU-D-02 — Distribuir bloques de fotos entre los nodos del grupo](https://github.com/user-attachments/assets/59e01d23-0709-4e21-9cc5-8d78c9a8dc1c)

#### CU-D-04 — Procesar bloque de fotos localmente
El nodo colaborador carga el modelo MobileNetV3 en memoria, clasifica cada fotografía del bloque recibido, calcula la confianza estadística y retorna las etiquetas generadas al nodo anfitrión[cite: 19].

![CU-D-04 — Procesar bloque de fotos localmente](https://github.com/user-attachments/assets/8be4ad5e-e6ea-48a8-9060-fc95ee84c419)

#### CU-D-06 — Consolidar resultados y notificar álbum listo
El orquestador unifica las clasificaciones recibidas, agrupa las fotos en subálbumes temáticos, genera el resumen de la sesión y emite la notificación de finalización a todos los participantes[cite: 19].

![CU-D-06 — Consolidar resultados y notificar álbum listo](https://github.com/user-attachments/assets/575a1e9c-70c9-4983-9ec2-304af15ff0c8)

---

### Módulos Complementarios

* **Módulo A — Cuenta y Perfil de Usuario:** Registro de cuentas, inicio de sesión seguro, control de privacidad y activación manual o automática del modo colaborador en segundo plano[cite: 16, 20, 23].
* **Módulo E — Categorización y Visualización:** Vista interactiva del álbum terminado, segmentado en subálbumes temáticos (playa, comida, reuniones, atardeceres) con la opción de reclasificar imágenes manualmente si la confianza estadística del modelo fue baja[cite: 21, 23].
* **Módulo F — Descarga y Difusión:** Opciones para exportar el álbum completo a la memoria del teléfono, descargar únicamente una categoría específica o compartir imágenes individuales en resolución nativa[cite: 21].

---

## 8. Atributos de Calidad y Restricciones Técnicas

El diseño técnico responde a métricas y límites de ingeniería establecidos para garantizar estabilidad y rendimiento[cite: 6, 7, 24, 25]:

* **Tiempo de Inferencia Extremo a Extremo:** La clasificación por bloque no debe sobrepasar los 1.500 milisegundos en redes inalámbricas con un ancho de banda mayor o igual a 20 Mbps y tiempos de respuesta de red menores o iguales a 15 ms[cite: 24, 25].
* **Tiempo de Serialización:** La transformación y empaquetado binario mediante Protocol Buffers no debe tomar más de 80 milisegundos de tiempo de procesador[cite: 25].
* **Tiempo de Descubrimiento de Red:** El escaneo y resolución mDNS de hasta 15 terminales concurrentes debe realizarse en un máximo de 2.500 milisegundos[cite: 25].
* **Especificaciones del Dispositivo Móvil:** Teléfonos con Android 10 (API 29) o superior (soporte base desde API 26), un mínimo de 3 GB de memoria RAM libre para mitigar el cierre de procesos en segundo plano, 100 MB de espacio de almacenamiento disponible y cámara trasera de al menos 8 megapíxeles[cite: 6, 7, 8, 25].

---

## 9. Beneficios e Impacto del Proyecto

1. **Privacidad integral:** Los archivos multimedia de los usuarios se transmiten únicamente dentro del perímetro físico de la red inalámbrica local; las imágenes personales jamás se cargan ni almacenan en servidores remotos en la nube[cite: 3, 6, 8].
2. **Independencia de la conexión a internet:** El flujo intensivo de cómputo, clasificación y distribución opera de manera autónoma sobre la red local Wi-Fi, eliminando costos por uso de paquetes de datos móviles[cite: 3, 6, 8].
3. **Eficiencia y reducción de tiempos:** Al segmentar y procesar en paralelo las fotografías aprovechando la potencia combinada de múltiples dispositivos, se reducen notablemente los tiempos de espera en comparación con la ejecución en un solo terminal[cite: 3, 5, 23].
4. **Entrega automatizada del producto:** Los participantes del evento obtienen de forma inmediata un álbum curado, ordenado temáticamente y libre de capturas defectuosas o redundantes[cite: 3].

## Flujo de aplicación

https://github.com/user-attachments/assets/3de3035c-bbd0-4337-81e3-94d302a39d53


