import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';
export default defineConfig(function (_a) {
    var mode = _a.mode;
    var env = loadEnv(mode, '.', '');
    return {
        plugins: [vue()],
        server: {
            port: 5173,
            proxy: {
                '/delay-queue': {
                    target: env.VITE_API_TARGET || 'http://localhost:8080',
                    changeOrigin: true
                }
            }
        }
    };
});
