#!/bin/bash -evu

if [ "${TRAVIS:-false}" == true ]; then
    openssl aes-256-cbc -K $encrypted_287f75608f39_key -iv $encrypted_287f75608f39_iv -in .travis/github-deploy.key.enc -out .travis/github-deploy.key -d
    chmod 600 .travis/github-deploy.key
    eval $(ssh-agent -s)
    ssh-add .travis/github-deploy.key
fi

TMP_USERNAME=$(git config --global user.name)
TMP_EMAIL=$(git config --global user.email)

git config --global user.name "chkpnt CI"
git config --global user.email chkpnt-ci@chkpnt.de

./gradlew docs gitPublishPush

git config --global user.name "$TMP_USERNAME"
git config --global user.email "$TMP_EMAIL"
