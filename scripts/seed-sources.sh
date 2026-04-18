#!/usr/bin/env bash
set -e

psql postgresql://aipostman:aipostman@localhost:5432/aipostman -f "$(dirname "$0")/../sql/init.sql"
