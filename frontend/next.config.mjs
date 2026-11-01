/** The browser calls the local application only; Spring owns the API. */
const backend = process.env.ELECTION_API_ORIGIN || 'http://127.0.0.1:8080';
export default {
  async rewrites() { return [{ source: '/api/v1/:path*', destination: `${backend}/api/v1/:path*` }]; }
};
