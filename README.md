ipcam-archive-viewer
====================

A node.js+express based server for viewing archived images downloaded from a security camera. Designed for and tested with a Foscam FI8910W (http://foscam.us/foscam-fi8910w-wireless-ip-camera.html). Other cameras _should_ work if the image can be downloaded via a GET request.

### Requirements
* node - http://nodejs.org/
* leiningen - http://leiningen.org/

### How do I use it?
1. git clone
2. npm install
3. lein cljsbuild once
4. cp example_config.cljs config.cljs (edit with your settings)
5. ./start.sh
