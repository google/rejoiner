// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

const React = require('react');

class Footer extends React.Component {
  docUrl(doc) {
    const baseUrl = this.props.config.baseUrl;
    const language = this.props.config.language;
    return baseUrl + 'docs/' + (language ? language + '/' : '') + doc;
  }
  render() {
    const {
      baseUrl,
      footerIcon,
      title,
      repoUrl
    } = this.props.config;
    return (
      <footer className="nav-footer" id="footer">
        <section className="sitemap">
          <a href={baseUrl} className="nav-home">
            {footerIcon && (
              <img
                src={baseUrl + footerIcon}
                alt={title}
                width="62"
                height="55"
              />
            )}
          </a>
          <div>
            <h5>Docs</h5>
            <a href={this.docUrl('queries.html')}>
              Getting Started
            </a>
          </div>
          <div>
            <h5>Community</h5>
            <a
              href="http://stackoverflow.com/questions/tagged/rejoiner"
              target="_blank">
              Stack Overflow
            </a>
          </div>
          <div>
            <h5>More</h5>
            <a href="https://github.com/google/rejoiner">GitHub</a>
            <a
              className="github-button"
              href={repoUrl}
              data-icon="octicon-star"
              data-count-href="/google/rejoiner/stargazers"
              data-show-count={true}
              data-count-aria-label="# stargazers on GitHub"
              aria-label="Star this project on GitHub">
              Star
            </a>
          </div>
        </section>
        <section className="copyright">
          Copyright &copy; {new Date().getFullYear()} Google LLC.
        </section>
      </footer>
    );
  }
}

module.exports = Footer;
