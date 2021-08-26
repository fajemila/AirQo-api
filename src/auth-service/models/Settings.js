const mongoose = require("mongoose").set("debug", true);
const ObjectId = mongoose.Types.ObjectId;
var uniqueValidator = require("mongoose-unique-validator");
const { logElement, logText, logObject } = require("../utils/log");
const isEmpty = require("is-empty");
const jsonify = require("../utils/jsonify");
const HTTPStatus = require("http-status");

const minValue = 0;
const getPeriod = ({ maxValue }) => ({
  type: new Schema(
    {
      value: {
        type: Number,
        min: minValue,
        max: maxValue,
      },
      maxValue: Number,
    },
    { _id: false }
  ),
  required: true,
  default: {
    value: minValue,
    maxValue,
  },
});

const periodSchema = new mongoose.Schema(
  {
    value: { type: String },
    label: { type: String },
    unitValue: getPeriod({ maxValue: 30 }),
    unit: { type: String },
  },
  { _id: false }
);

const chartSchema = new mongoose.Schema(
  {
    pollutant: {
      type: String,
      trim: true,
    },
    frequency: {
      type: String,
    },
    startDate: {
      type: Date,
    },
    endDate: {
      type: Date,
    },
    chartType: {
      type: String,
    },
    chartTitle: {
      type: String,
    },
    chartSubTitle: {
      type: String,
    },
    airqloud_ids: [
      {
        type: ObjectId,
      },
    ],
    airqlouds: [
      {
        type: String,
      },
    ],
    sites: [
      {
        type: String,
      },
    ],
    site_ids: [
      {
        type: ObjectId,
      },
    ],
    period: {
      type: periodSchema,
      default: {},
    },
  },
  { _id: false }
);

const activitiesSchema = new mongoose.Schema(
  {
    activity_type: {
      type: String,
    },
    next_maintenance_period: {
      type: Number,
    },
  },
  { _id: false }
);

const devicesSchema = new mongoose.Schema(
  {
    generation: { type: Number },
    powerTypes: [{ type: String }],
    mountTypes: [{ type: String }],
  },
  { _id: false }
);

const rolesSchema = new mongoose.Schema(
  {
    name: {
      type: String,
    },
    resources: {
      type: [{ type: String }],
    },
  },
  { _id: false }
);

const SettingsSchema = new mongoose.Schema({
  user_id: {
    type: ObjectId,
  },
  user: {
    type: ObjectId,
    required: [true, "user is required"],
  },
  organisation: {
    type: ObjectId,
    required: [true, "organisation is required"],
  },
  /** the chartTitle and subTitle */
  charts: {
    type: chartSchema,
    default: {},
  },
  /** the activity_type */
  activities: {
    type: activitiesSchema,
    default: {},
  },
  /** the generation */
  devices: {
    type: devicesSchema,
    default: {},
  },
  /*** the role name */
  roles: {
    type: rolesSchema,
    default: {},
  },
});

SettingsSchema.plugin(uniqueValidator, {
  message: `{VALUE} is a duplicate value!`,
});

SettingsSchema.index(
  {
    user: 1,
    user_id: 1,
  },
  {
    unique: true,
  }
);

SettingsSchema.methods = {
  toJSON() {
    return {
      _id: this._id,
      pollutant: this.pollutant,
      frequency: this.frequency,
      user: this.user,
      user_id: this.user_id,
      airqloud_id: this.airqloud_id,
      airqloud: this.airqloud,
      startDate: this.startDate,
      endDate: this.endDate,
      chartType: this.chartType,
      chartTitle: this.chartTitle,
      chartSubTitle: this.chartSubTitle,
      sites: this.sites,
      period: this.period,
      site_ids: this.site_ids,
    };
  },
};

SettingsSchema.statics = {
  async register(args) {
    try {
      let body = args;
      let data = await this.create({
        ...body,
      });

      if (!isEmpty(data)) {
        return {
          success: true,
          data,
          message: "default created",
          status: HTTPStatus.OK,
        };
      } else {
        return {
          success: false,
          message: "default not created despite successful operation",
          status: HTTPStatus.CREATED,
        };
      }
    } catch (err) {
      let e = jsonify(err);
      let response = {};
      logObject("the err", e);
      let errors = {};
      let message = "Internal Server Error";
      let status = HTTPStatus.INTERNAL_SERVER_ERROR;
      if (err.code === 11000 || err.code === 11001) {
        errors = err.keyValue;
        message = "duplicate values provided";
        status = HTTPStatus.CONFLICT;
      } else {
        message = "validation errors for some of the provided fields";
        status = HTTPStatus.CONFLICT;
        errors = err.errors;
        Object.entries(err.errors).forEach(([key, value]) => {
          return (response[key] = value.message);
        });
      }
      return {
        errors: response,
        message,
        success: false,
        status,
      };
    }
  },
  async list({ skip = 0, limit = 20, filter = {} } = {}) {
    try {
      logObject("the filter in the defaults", filter);
      let defaults = await this.find(filter)
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limit)
        .exec();
      let data = jsonify(defaults);
      logObject("the data for defaults", data);
      if (!isEmpty(data)) {
        return {
          success: true,
          data,
          message: "successfully listed the defaults",
        };
      }
      if (isEmpty(data)) {
        return {
          success: true,
          message: "no defaults exist",
          data,
        };
      }
      return {
        success: false,
        message: "unable to retrieve defaults",
        data,
      };
    } catch (error) {
      return {
        success: false,
        message: "unable to list the defaults",
        error: error.message,
      };
    }
  },
  async modify({ filter = {}, update = {} } = {}) {
    try {
      let options = { new: true };
      let udpatedDefault = await this.findOneAndUpdate(
        filter,
        update,
        options
      ).exec();

      let data = jsonify(udpatedDefault);
      logObject("updatedDefault", data);

      if (!isEmpty(data)) {
        return {
          success: true,
          message: "successfully modified or created the default",
          data,
          status: HTTPStatus.OK,
        };
      } else {
        return {
          success: false,
          message: "defaults do not exist, please crosscheck",
          status: HTTPStatus.NOT_FOUND,
        };
      }
    } catch (err) {
      let error = {};
      let message = "";
      let status = "";
      if (err.code == 11000) {
        error = err.keyValue;
        message = "duplicate values provided";
        status = HTTPStatus.CONFLICT;
      }
      return {
        success: false,
        message,
        error,
        status,
      };
    }
  },
  async remove({ filter = {} } = {}) {
    try {
      let options = {
        projection: {
          _id: 1,
          user: 1,
          user_id: 1,
          chartTitle: 1,
          chartSubTitle: 1,
        },
      };
      let removedDefault = await this.findOneAndRemove(filter, options).exec();
      logElement("removedDefault", removedDefault);
      let data = jsonify(removedDefault);
      if (!isEmpty(data)) {
        return {
          success: true,
          message: "successfully removed the default",
          data,
          status: HTTPStatus.OK,
        };
      } else {
        return {
          success: false,
          message: "default does not exist, please crosscheck",
          status: HTTPStatus.NOT_FOUND,
        };
      }
    } catch (error) {
      return {
        success: false,
        message: "model server error",
        error: error.message,
        status: HTTPStatus.INTERNAL_SERVER_ERROR,
      };
    }
  },
};

module.exports = SettingsSchema;
