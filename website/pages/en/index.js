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

const React = require("react")
const siteConfig = require(process.cwd() + "/siteConfig.js")

const Button = props => (
  <div className="pluginWrapper buttonWrapper">
    <a className="button" href={props.href} target={props.target || "_self"}>
      {props.children}
    </a>
  </div>
)

const SplashContainer = props => (
  <div className="homeContainer">
    <div className="homeSplashFade">
      <div className="wrapper homeWrapper">{props.children}</div>
    </div>
  </div>
)

const PromoSection = props => (
  <div className="section promoSection">
    <div className="promoRow">
      <div className="pluginRowBlock">{props.children}</div>
    </div>
  </div>
)

const imgUrl = img => siteConfig.baseUrl + img

class Index extends React.Component {
  docUrl(doc) {
    const language = this.props.language || ""
    return siteConfig.baseUrl + "docs/" + (language ? language + "/" : "") + doc
  }
  render() {
    return (
      <div>
        <SplashContainer>
          <div className="projectLogo">
            <img src={imgUrl("rejoiner_logo.svg")} />
          </div>
          <div className="inner">
            <h2 className="projectTitle">
              {siteConfig.title}
              <small>{siteConfig.tagline}</small>
            </h2>
            <PromoSection>
              <Button href={this.docUrl("queries.html")}>Get Started</Button>
              <Button href="https://github.com/google/rejoiner">GitHub</Button>
            </PromoSection>
          </div>
        </SplashContainer>
        <div className="mainContainer alignCenter">
          <img
            src={imgUrl("rejoiner_overview.svg")}
            style={{ maxWidth: 750, width: "100%" }}
          />
        </div>
      </div>
    )
  }
}

module.exports = Index
