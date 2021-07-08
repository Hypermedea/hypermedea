const http = require('http');
const fs = require('fs');

let td = fs.readFileSync('td.ttl', 'utf-8');

let status = false;

let srv = http.createServer((req, resp) => {
    try {
        if (req.method == 'GET' && req.url == '/td') {
            console.log('>> GET /td');

            resp.setHeader('Content-Type', 'text/turtle');
            resp.statusCode = 200;
            resp.end(td);

            console.log('<< <TD>');
        } else if (req.method == 'GET' && req.url == '/status') {
            console.log('>> GET /status');

            resp.statusCode = 200;
            resp.end(JSON.stringify(status));

            console.log('<< ' + status);
        } else if (req.method == 'PUT' && req.url == '/status') {
            console.log('>> PUT /status');

            let payload = null;

            req.setEncoding('utf-8');
            req.on('readable', () => payload = Boolean(req.read()));

            req.on('end', () => {
                console.log('>> ' + payload);
                if (payload instanceof Boolean) status = payload;

                resp.statusCode = 204;
                resp.end();
            });
        } else if (req.method == 'PUT' && req.url == '/toggle') {
            console.log('>> PUT /toggle');

            status = !status;

            resp.statusCode = 204;
            resp.end();
        } else {
            resp.statusCode = 400;
            resp.end();
        }
    } catch (e) {
        resp.statusCode = 500;
        resp.end(e);
    }
});

srv.listen(8080);

console.log('Server started. See http://localhost:8080/td...');