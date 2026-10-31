# Cómo subir cambios a GitHub

Esta guía es para el repositorio `https://github.com/JeffChoc/SIGELAB` y asume que ya tienes una
cuenta de GitHub y permiso de escritura (apareces como colaborador del repositorio).

> Trabaja siempre dentro de la carpeta que contiene `.git` y `pom.xml`:
> `SIGELAB` (no la carpeta superior `SIGELAB!`).

## 1. Configura tu identidad de Git

Git toma el autor de cada commit de la configuración local. Si el equipo comparte la computadora,
configura **tu** nombre y **tu** correo de GitHub solo para este repositorio:

```bash
git config user.name "Tu Nombre"
git config user.email "tu-correo-de-github@example.com"
```

Puedes verificarlo con:

```bash
git config user.name
git config user.email
```

Cada commit mostrará ese autor y quedará asociado a tu cuenta si el correo coincide con el de
GitHub (o con uno agregado en *Settings → Emails*).

## 2. Actualiza tu copia antes de trabajar

```bash
git switch main
git pull origin main
```

## 3. Crea una rama para tus cambios

No trabajes directamente sobre `main`:

```bash
git switch -c mejoras-login-usuarios-auditoria
```

## 4. Revisa y prepara los archivos

```bash
git status
git diff
```

Agrega únicamente los archivos que corresponden a tus cambios:

```bash
git add src/main/java/universidad/sigelab/service/AutenticacionService.java
git add src/main/java/universidad/sigelab/service/UsuarioService.java
git add src/main/java/universidad/sigelab/service/AuditoriaService.java
# ... y los demás archivos modificados
```

> Nunca agregues `config.properties`, `target/`, `.idea/`, `facial/rostros/` ni archivos con
> contraseñas. El `.gitignore` ya los excluye, pero revisa `git status` antes de confirmar.

## 5. Crea el commit

```bash
git commit -m "mejora: seguridad de login, gestión de usuarios y auditoría"
```

Puedes comprobar el autor antes de subir:

```bash
git log -1 --format="%an <%ae> %s"
```

## 6. Sube tu rama

```bash
git push -u origin mejoras-login-usuarios-auditoria
```

GitHub pedirá autenticación. No uses tu contraseña normal: crea un **Personal Access Token**
(*GitHub → Settings → Developer settings → Personal access tokens*) y úsalo como contraseña, o
configura SSH.

## 7. Abre un Pull Request

1. Entra a `https://github.com/JeffChoc/SIGELAB`.
2. GitHub mostrará el botón **Compare & pull request** para tu rama.
3. Verifica que el destino sea `main` y el origen tu rama.
4. Escribe un título y una descripción breve de los cambios.
5. Crea el Pull Request y avisa a tu compañero para que lo revise y lo fusione.

Al fusionarse, tus commits quedan en `main` con tu nombre de autor y tu usuario de GitHub.

## Alternativa si no eres colaborador

Si no puedes hacer `push` al repositorio de tu compañero:

1. En GitHub, entra al repositorio y pulsa **Fork**.
2. Clona **tu fork** y crea ahí tu rama.
3. Sube la rama a tu fork.
4. Abre un Pull Request desde tu fork hacia el repositorio original.

## Comandos de emergencia

```bash
# Ver qué archivos cambiaron
git status

# Ver el remoto configurado
git remote -v

# Deshacer el último commit sin perder los archivos
git reset --soft HEAD~1

# Descartar cambios de un archivo (¡cuidado!)
git restore archivo.java
```

Si `git push` falla porque el remoto tiene commits nuevos, primero actualiza tu rama:

```bash
git pull --rebase origin main
git push -u origin mejoras-login-usuarios-auditoria
```
