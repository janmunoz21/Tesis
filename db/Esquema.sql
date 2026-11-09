CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE usuarios (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre          VARCHAR(100) NOT NULL,
  correo          VARCHAR(255) NOT NULL UNIQUE,
  contrasena_hash VARCHAR(255) NOT NULL,
  foto_perfil_url VARCHAR(500)            -- CU-A-05 (Baja), puede quedar NULL
);

CREATE TABLE albumes (
  id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre           VARCHAR(150) NOT NULL,
  descripcion      VARCHAR(500),
  creador_id       UUID NOT NULL REFERENCES usuarios(id),
  fecha_creacion   TIMESTAMPTZ NOT NULL DEFAULT now(),
  token_invitacion VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE miembros_album (
  album_id   UUID NOT NULL REFERENCES albumes(id) ON DELETE CASCADE,
  usuario_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
  rol        VARCHAR(20) NOT NULL DEFAULT 'PARTICIPANTE'
             CHECK (rol IN ('CREADOR','PARTICIPANTE')),
  estado     VARCHAR(20) NOT NULL DEFAULT 'ACTIVO'
             CHECK (estado IN ('PENDIENTE','ACTIVO')),
  PRIMARY KEY (album_id, usuario_id)
);

CREATE TABLE fotos_metadatos (
  id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  album_id             UUID NOT NULL REFERENCES albumes(id) ON DELETE CASCADE,
  propietario_id       UUID NOT NULL REFERENCES usuarios(id),
  etiqueta_consolidada VARCHAR(100),
  porcentaje_confianza REAL CHECK (porcentaje_confianza BETWEEN 0 AND 1),
  reclasificada_manual BOOLEAN NOT NULL DEFAULT FALSE
);

-- Índices que pide el SRS (3.6.3)
CREATE INDEX idx_miembros_usuario_album ON miembros_album(usuario_id, album_id);
CREATE INDEX idx_fotos_album_etiqueta  ON fotos_metadatos(album_id, etiqueta_consolidada);