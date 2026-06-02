FROM harbor.lab:8080/library/node:13.12.0-alpine
WORKDIR /app
ENV PATH /app/node_modules/.bin:$PATH
COPY .npmrc ./
COPY package.json ./
COPY package-lock.json ./
RUN npm install --silent
RUN npm install react-scripts@3.4.1 -g --silent
COPY . ./

# Run as non-root (node:alpine has a built-in `node` user)
USER node

CMD ["npm", "start"]
