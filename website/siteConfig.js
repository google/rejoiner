const siteConfig = {
  algolia: {
    apiKey: '0cd38f3b9cf50238665b8981b9709451',
    indexName: 'rejoiner',
  },
  title: 'Rejoiner',
  tagline: 'Uniform GraphQL schema from gRPC microservices',
  url: 'https://rejoiner.io',
  baseUrl: '/',
  projectName: 'site',
  headerLinks: [
    {doc: 'queries', label: 'Docs'},
    {href: 'https://github.com/google/rejoiner', label: 'GitHub'},
  ],
  users: [],
  headerIcon: 'rejoiner_logo_white.svg',
  footerIcon: 'rejoiner_logo_white.svg',
  favicon: 'favicon.png',
  colors: {
    primaryColor: '#039be5',
    secondaryColor: '#0277bd',
  },
  copyright: 'Copyright © ' + new Date().getFullYear() + ' Google LLC',
  organizationName: 'google',
  projectName: 'rejoiner',
  highlight: {
    theme: 'default',
  },
  scripts: ['https://buttons.github.io/buttons.js'],
  repoUrl: 'https://github.com/google/rejoiner',
  gaTrackingId: 'UA-109799776-2',
  cname: 'rejoiner.io'
};

module.exports = siteConfig;
